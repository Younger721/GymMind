# Task 2 report: coach member request workflow

Implemented the stateful coach/member relation model and service operations.

## Changes

- Added `CoachAssignmentStatus` with `PENDING`, `ACTIVE`, `REJECTED`, and `ENDED`.
- Added requester and request/resolution timestamps to `CoachAssignment` and legal state transitions.
- Extended repository adapters with tenant scoped relation lookup, pending lookup, status existence checks, and status based deletion.
- Added `request`, `accept`, `reject`, `cancel`, and `end` to `CoachAssignmentService`.
- Member actors can request a coach; coach actors can accept/reject only their own requests; owners can cancel/end their relations.
- `canAccess` now checks only `ACTIVE` relations in the current tenant.
- Kept legacy gym administrator `assign`/`unassign` APIs for compatibility with existing callers.

## Verification

The focused Maven test command was attempted, but the repository currently fails compilation in many unrelated pre-existing files (agent, AI, booking, audit, and other packages). The errors occur before the focused coach test is compiled, so a green test result is not currently available.

