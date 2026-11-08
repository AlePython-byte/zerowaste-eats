-- Supabase exposes the public schema through its Data API to anyone who holds the
-- publishable key, and that key ships inside the mobile app. All data access goes
-- through the backend instead, so row level security is enabled with no policies:
-- the Data API roles (anon, authenticated) can read and write nothing.
--
-- The backend is not affected: it connects as the owner of these tables, and owners
-- bypass row level security unless it is forced, which it is not here.

ALTER TABLE "users" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "merchant_profiles" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "establishments" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "offers" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "reservations" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "favorites" ENABLE ROW LEVEL SECURITY;

-- Prisma's own migration history lives in the same schema
ALTER TABLE "_prisma_migrations" ENABLE ROW LEVEL SECURITY;
