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

## Review follow-up

- Removed duplicate Spring Data method declarations.
- Request identity resolves the member profile by actor user id and requires a `member_tenant` row for the active tenant.
- Pending rejection is restricted to the owning coach; member actors can only cancel their own pending request.
- Follow-up fixes resolve member ownership through the member profile ID (rather than comparing user ID to profile ID), and Spring injection now selects the membership-aware constructor.
