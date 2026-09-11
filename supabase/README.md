# Supabase templates

This folder contains the Supabase-related templates and schema for the Qarz daftari app.

## Contents

- `schema.sql` — database schema, policies, and helper objects for debts storage
- `templates/` — HTML email templates for Supabase Authentication

## Email templates

Use the files in `supabase/templates/` inside the Supabase Dashboard:

- Authentication → Email Templates → Confirm signup
- Authentication → Email Templates → Invite user
- Authentication → Email Templates → Magic link / OTP
- Authentication → Email Templates → Change email address
- Authentication → Email Templates → Reset password
- Authentication → Email Templates → Reauthentication

## Notes

- Supabase email templates use variables such as `{{ .ConfirmationURL }}`, `{{ .Token }}`, and `{{ .SiteURL }}`.
- Keep the templates short, branded, and readable on mobile devices.
- Do not expose internal error details in the frontend. Use generic messages there.
