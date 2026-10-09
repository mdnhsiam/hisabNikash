"""Query OSV using only resolved public Maven coordinates; fail on findings/errors."""

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import urllib.error
import urllib.request


def check(inventory_path, output_path):
    inventory = json.loads(inventory_path.read_text(encoding="utf-8-sig"))
    packages = inventory["packages"]
    if not packages:
        raise ValueError("Refusing an empty runtime inventory")
    for package in packages:
        if (package.get("ecosystem") != "Maven" or
                not isinstance(package.get("name"), str) or
                len(package["name"].split(":")) != 2 or
                not isinstance(package.get("version"), str) or
                not package["version"]):
            raise ValueError("Inventory must contain public Maven names and exact versions")

    findings = []
    for offset in range(0, len(packages), 100):
        batch = packages[offset:offset + 100]
        request_body = {"queries": [
            {"package": {"ecosystem": "Maven", "name": p["name"]}, "version": p["version"]}
            for p in batch
        ]}
        request = urllib.request.Request(
            "https://api.osv.dev/v1/querybatch",
            data=json.dumps(request_body).encode("utf-8"),
            headers={"Content-Type": "application/json", "User-Agent": "HisabNikash-runtime-advisory-check/1"},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=60) as response:
            results = json.load(response)["results"]
        if len(results) != len(batch):
            raise ValueError("OSV returned an incomplete batch")
        for package, result in zip(batch, results):
            # Batch responses may paginate: an incomplete result must not pass.
            if result.get("next_page_token"):
                raise ValueError("OSV result requires pagination; review manually before release")
            findings.append({"package": package, "advisories": result.get("vulns", [])})

    affected = [result for result in findings if result["advisories"]]
    report = {
        "checked_at_utc": datetime.now(timezone.utc).isoformat(),
        "endpoint": "https://api.osv.dev/v1/querybatch",
        "configuration": inventory["configuration"],
        "package_count": len(packages),
        "affected_package_count": len(affected),
        "status": "advisories-found" if affected else "no-known-advisories-returned",
        "limitations": "OSV coverage is not proof of absence of vulnerabilities; excludes OS, build tooling and debug/test-only dependencies.",
        "results": findings,
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"OSV checked {len(packages)} runtime packages; {len(affected)} affected packages. Report: {output_path}")
    for result in affected:
        ids = ", ".join(advisory["id"] for advisory in result["advisories"])
        print(f"{result['package']['name']}:{result['package']['version']}: {ids}")
    return 1 if affected else 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inventory", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    try:
        sys.exit(check(args.inventory, args.output))
    except (OSError, ValueError, KeyError, urllib.error.URLError) as error:
        print(f"Advisory check failed (not a clean result): {error}", file=sys.stderr)
        sys.exit(2)
