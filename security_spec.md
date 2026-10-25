# DoorCut Security Specification (Payload-First TDD)

## 1. Data Invariants
1. **Default Deny**: All unlisted collections/documents reject all reads and writes.
2. **No Self-Admin Escalation**: Non-admin users cannot create a `/users/{uid}` document with `role == 'ADMIN'` or `status == 'ACTIVE'` when registering as `BARBER` (barbers start as `PENDING`). Only verified owner email `nomaankhannfs2@gmail.com` or existing `/admins/{uid}` is recognized as Admin.
3. **PII Isolation**: `/users/{userId}` and `/users/{userId}/addresses/{addressId}` can only be read by `request.auth.uid == userId` or `isAdmin()`.
4. **Protected Home Address Unlock**: `/bookings/{bookingId}/privateDetails/{detailId}` can ONLY be read by the booking's `customerId`, `isAdmin()`, or the assigned `barberId` when the parent `/bookings/{bookingId}` exists and is not cancelled/expired. Unassigned barbers cannot scrape customer addresses.
5. **Dual-Payment Integrity**: `totalCustomerCostPaise == barberServicePayablePaise + platformBookingFeePaise` and `barberServicePayablePaise == serviceSubtotalPaise + approvedAddOnPaise + travelChargePaise`. Platform fee is never subtracted from barber payable.
6. **Extra Service Approval Gate**: Barbers cannot unilaterally increase `approvedAddOnPaise` or `barberServicePayablePaise`; they can only submit a `pendingAddOnStatus == 'REQUESTED'`, which only the `customerId` (or `isAdmin()`) can transition to `'APPROVED'`.
7. **Query Enforcer**: Every `allow list` rule evaluates `resource.data` (e.g., `resource.data.customerId == request.auth.uid`, `resource.data.barberId == request.auth.uid`, `resource.data.isActive == true`, `resource.data.approvalStatus == 'APPROVED'`, or `isAdmin()`), never allowing blanket `isSignedIn()` scraping of private collections.
8. **Append-Only Audit Logs**: `/auditLogs/{logId}` can only be created by `isAdmin()`, and never updated or deleted (`allow update, delete: if false`).

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Catalog Scrape**: `unauth.collection("services").where("isActive", "==", true).get()` -> REJECTED.
2. **Privilege Escalation on Registration**: `alice` writes `/users/alice` with `role: "ADMIN"` -> REJECTED.
3. **Self-Approving Barber**: `bob` writes `/barberProfiles/bob` with `approvalStatus: "APPROVED"` -> REJECTED (`PENDING` required for non-admin creation).
4. **Shadow Ghost Field Injection**: `alice` updates `/users/alice` adding `isSuperUser: true` -> REJECTED via `affectedKeys().hasOnly(...)` and `keys().hasOnly(...)`.
5. **Cross-User Address Theft**: `bob` reads `/users/alice/addresses/addr1` -> REJECTED.
6. **Unassigned Barber Reading Booking Home Address**: `charlie` (another barber) reads `/bookings/b1/privateDetails/main` -> REJECTED.
7. **Unfiltered Booking List Scrape**: `alice` runs `db.collection("bookings").get()` without `.where("customerId", "==", "alice")` -> REJECTED.
8. **Silent Barber Price Hike**: Assigned barber `bob` updates `/bookings/b1` directly modifying `barberServicePayablePaise` from `55000` to `90000` -> REJECTED.
9. **Spoofed Email Admin Bypass**: Attacker with `email: "nomaankhannfs2@gmail.com"` and `email_verified: false` attempts admin service creation -> REJECTED.
10. **Terminal Booking Mutation**: `bob` attempts to change status of a `CANCELLED` booking back to `IN_PROGRESS` -> REJECTED.
11. **Value Poisoning (1MB String)**: `alice` writes a 10,000-char `customerNotes` into `/bookings/b1` -> REJECTED via `.size() <= 500`.
12. **Audit Log Tampering**: Even `admin` attempts to delete or update `/auditLogs/log1` -> REJECTED (`allow update, delete: if false`).
