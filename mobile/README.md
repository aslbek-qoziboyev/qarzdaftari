# Qarz Daftari Mobile

Native Android application built with Kotlin + Jetpack Compose.

Package: com.qarzdaftari.aslbek

Backend: Supabase Auth + Postgres.
Authentication: Google OAuth through Supabase.
Callback: qarzdaftari://auth/callback

Open the mobile folder in Android Studio as a Gradle project.

Before running, replace SUPABASE_KEY in app/build.gradle.kts with the Supabase publishable/anon key. Never put a service_role or secret key in the app.