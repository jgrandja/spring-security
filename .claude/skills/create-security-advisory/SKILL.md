---
name: create-security-advisory
description: Generate a Security Advisory using a standardized template. Use when the user asks to draft a CVE report, security advisory, or document a vulnerability.
---

# Create Security Advisory

## Overview
This skill provides a standardized template and workflow for generating Security Advisories for documenting software vulnerabilities.

## Instructions
When the user asks to create a Security Advisory or document a vulnerability:

1. **Gather Information**: Ensure you have the necessary details from the user:
   - CVE ID (if assigned)
   - Title
   - Published date
   - Severity
   - Description (focusing on impact and preconditions, avoiding exact reproducible exploits)
   - Affected Products and Versions
   - Mitigation (fixed versions)
   - Credit (reporter information)
   - References (CVSS score)
2. **Format**: Apply the information to the **Template** provided below.
3. **Output**: Write the result to a Markdown file, named after the CVE ID (e.g., `CVE-YYYY-XXXXX.md`).

## Template

Use the following Markdown structure for the Security Advisory:

```markdown
---
title: '[CVE ID]: [Title]'
publishedAt: [Published date (e.g., YYYY-MM-DD)]
severity: [Severity]
---

# Description

[Provide a clear and concise description of the vulnerability. Explain the vulnerable component, the insufficient validation or flaw, the preconditions, and the potential exploit scenarios (e.g., XSS, SSRF, Privilege Escalation). Keep the description high-level to avoid providing exact reproducible exploit payloads.]

# Affected Spring Products and Versions

[Product Name]:

- [Affected Version Range 1]

# Mitigation

Users of affected versions should upgrade to the corresponding fixed version.

| Affected version(s) | Fix version | Availability |
| --- | --- | --- |
| [Affected Version Branch (e.g., 7.0.x] | [Fixed Version] | [Availability (e.g., OSS or [Commercial](https://enterprise.spring.io/))] |

# Credit

The issue was identified and responsibly reported by [Reporter Name] ([@username]([Link to profile])).

# References
- [Link to CVSS score]
```

## Example

**Input**: "Please draft a Security Advisory for CVE-2026-22752 involving Spring Security..."

**Output**:

```markdown
---
title: 'CVE-2026-22752: Spring Security Authorization Server Dynamic Client Registration endpoints perform insufficient validation of client metadata'
publishedAt: 2026-04-21
severity: Critical
---

# Description

Spring Security Authorization Server Dynamic Client Registration endpoints perform insufficient validation of certain client metadata fields when explicitly enabled.

An attacker possessing a valid Initial Access Token can dynamically register a malicious client with crafted metadata.
Depending on the metadata provided and the Authorization Server's configuration, this can lead to Stored Cross-Site Scripting (XSS), Privilege Escalation, or Server-Side Request Forgery (SSRF).

# Affected Spring Products and Versions

Spring Security:

- 7.0.0 - 7.0.4

# Mitigation

Users of affected versions should upgrade to the corresponding fixed version.

| Affected version(s) | Fix version | Availability |
| --- | --- | --- |
| 7.0.x | 7.0.5 | OSS |

# Credit

The issue was identified and responsibly reported by John Smith ([@johnsmith](https://github.com/johnsmith)).

# References
- https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=AV:N/AC:L/PR:L/UI:N/S:C/C:H/I:H/A:N&version=3.1
```
