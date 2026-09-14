# Qarz Daftari Mobile

React Native + Expo mobile client for Qarz Daftari.

## Package
com.qarzdaftari.aslbek

## Authentication
Google OAuth through Supabase Auth. The app uses the custom callback scheme:

qarzdaftari://auth/callback

## Database
The mobile app uses the existing public.debts table and its RLS policies from ../supabase/schema.sql.

## Local setup

1. Install Node.js LTS and Android Studio.
2. From this directory run:
   npm install
3. Copy .env.example to .env and fill in the Supabase project URL and anon/publishable key.
4. Start:
   npm start
5. Press "a" in the Expo terminal to open Android.

## Google/Supabase setup

Google provider must be enabled in Supabase Authentication > Providers > Google.

For the Android OAuth client use:
- Package: com.qarzdaftari.aslbek
- SHA-1: the fingerprint of the Android debug/release signing certificate.

In Supabase Authentication > URL Configuration add:
qarzdaftari://auth/callback

Do not put the Supabase project URL in the app's visible login/callback UI. The user-facing action is "Qarz Daftariga kirish".
