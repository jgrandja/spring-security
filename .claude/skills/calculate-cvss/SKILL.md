---
name: calculate-cvss
description: Calculate the Common Vulnerability Scoring System (CVSS) Version 3.1 base score and vector string. Use when the user asks to calculate a CVSS score, evaluate vulnerability severity, or asks for CVSS metrics.
---

# Calculate CVSS Version 3.1 Score

## Overview
This skill provides a standardized workflow for calculating the CVSS Version 3.1 base score and generating the corresponding vector string for a vulnerability.

## Instructions
When the user asks to calculate a CVSS 3.1 score:

1. **Gather Metrics**: Assess the vulnerability against the 8 Base Metrics. If information is missing, ask the user to clarify or infer from the vulnerability description:
   - **Attack Vector (AV)**: Network (N), Adjacent (A), Local (L), Physical (P)
   - **Attack Complexity (AC)**: Low (L), High (H)
   - **Privileges Required (PR)**: None (N), Low (L), High (H)
   - **User Interaction (UI)**: None (N), Required (R)
   - **Scope (S)**: Unchanged (U), Changed (C)
   - **Confidentiality (C)**: None (N), Low (L), High (H)
   - **Integrity (I)**: None (N), Low (L), High (H)
   - **Availability (A)**: None (N), Low (L), High (H)

2. **Generate Vector String**: Combine the metrics into a standard CVSS 3.1 vector string format:
   `CVSS:3.1/AV:[X]/AC:[X]/PR:[X]/UI:[X]/S:[X]/C:[X]/I:[X]/A:[X]`

3. **Determine Score & Severity**: Provide the severity based on the CVSS v3.1 equations or known mapping (None: 0.0, Low: 0.1-3.9, Medium: 4.0-6.9, High: 7.0-8.9, Critical: 9.0-10.0). Since computing the exact decimal score manually can be complex, use a Python utility script if exact precision is required, or provide a link to the official calculator with the generated vector.

## Workflow

1. Discuss the vulnerability details with the user to determine each metric.
2. Clearly explain *why* each metric value was chosen.
3. Present the final vector string and the calculated severity level (Low, Medium, High, Critical).
4. Provide a link to the official calculator with the generated vector:
   `https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=[VECTOR]&version=3.1`

## Python CVSS Utility (Optional)
If you need to calculate the exact decimal score programmatically, you can write and execute a short Python script using the standard `cvss` library (`pip install cvss`).

```python
from cvss import CVSS3
vector = "CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:C/C:H/I:H/A:N"
c = CVSS3(vector)
print(f"Base Score: {c.base_score}, Severity: {c.severities()[0]}")
```

## Example

**Output Format**:

### CVSS 3.1 Metrics Evaluation
- **Attack Vector (AV)**: Network (N) - The vulnerability is exploitable over the network.
- **Attack Complexity (AC)**: Low (L) - No special access conditions or extenuating circumstances exist.
- **Privileges Required (PR)**: Low (L) - The attacker requires basic user privileges.
- **User Interaction (UI)**: None (N) - The system can be exploited without user interaction.
- **Scope (S)**: Changed (C) - Exploitation affects resources beyond the vulnerable component.
- **Confidentiality (C)**: High (H) - Total loss of confidentiality.
- **Integrity (I)**: High (H) - Total loss of integrity.
- **Availability (A)**: None (N) - No impact on availability.

**Vector**: `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:C/C:H/I:H/A:N`
**Base Score**: 9.3 (Critical)
**Calculator Link**: [NIST NVD CVSS v3.1 Calculator](https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=AV:N/AC:L/PR:L/UI:N/S:C/C:H/I:H/A:N&version=3.1)
