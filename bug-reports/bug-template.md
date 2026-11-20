# Defect report template

Copy this block into `documentation/defect-report.md` for each confirmed defect. Do not create an
entry until the behaviour has been reproduced on a second, independent execution.

```
### BUG-XXX — <short title>

- **Module:** <Authentication | Task management | Access control | Dashboard | UI>
- **Environment / build:** <e.g. backend commit/local build, frontend build, browser>
- **Severity:** <Critical | High | Medium | Low>   (impact if it reaches real users)
- **Priority:** <P1 | P2 | P3>                      (urgency to fix)
- **Linked test case:** TC-XXX-NNN
- **Preconditions:** <test account(s)/role(s)/data state used>

**Steps to reproduce**
1. ...
2. ...
3. ...

**Expected result:** ...

**Actual result:** ... (exact response/body/screenshot reference — never paraphrased into the
expected result)

**Evidence:** <path under bug-reports/evidence/, or a Postman/Newman output excerpt, with any
token/password/PII redacted>

**Status:** Open | In Progress | Fixed | Retest | Closed

**Verification after fix:** <date, re-run result, who/what verified it>
```
