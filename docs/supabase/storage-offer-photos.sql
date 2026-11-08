-- Offer photos in Supabase Storage: bucket and access policies.
--
-- Run once in the Supabase SQL editor (Database > SQL editor). It can be run again: it
-- updates the bucket settings and recreates its own policies.
--
-- Layout: offer-photos/<auth user id>/<file>.jpg|jpeg|webp
--   * Reading: public, by URL (the app shows offer photos to everyone). There is
--     deliberately no SELECT policy for anonymous users, so the bucket cannot be listed
--     to discover which businesses exist.
--   * Writing: a signed-in user can upload, replace and delete files only in the folder
--     named after their own user id, one level deep.
-- The backend matches this: an offer's imageUrl must be
--   <SUPABASE_URL>/storage/v1/object/public/offer-photos/<business user id>/<file>
-- (see server/src/offers/offer-image-url.ts).

-- Bucket: public reads, at most 2 MB, JPEG or WebP only (the app uploads JPEG)
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('offer-photos', 'offer-photos', true, 2097152, array['image/jpeg', 'image/webp'])
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

-- A path is the caller's when its only folder is their user id and the file is a photo
-- (storage.foldername returns the folders of a path, without the file name)

drop policy if exists "offer photos: upload to own folder" on storage.objects;
create policy "offer photos: upload to own folder"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'offer-photos'
  and array_length(storage.foldername(name), 1) = 1
  and (storage.foldername(name))[1] = (select auth.uid())::text
  and lower(storage.extension(name)) in ('jpg', 'jpeg', 'webp')
);

-- Needed to replace a file (upsert) and to list one's own photos
drop policy if exists "offer photos: read own folder" on storage.objects;
create policy "offer photos: read own folder"
on storage.objects for select
to authenticated
using (
  bucket_id = 'offer-photos'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "offer photos: replace in own folder" on storage.objects;
create policy "offer photos: replace in own folder"
on storage.objects for update
to authenticated
using (
  bucket_id = 'offer-photos'
  and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
  bucket_id = 'offer-photos'
  and array_length(storage.foldername(name), 1) = 1
  and (storage.foldername(name))[1] = (select auth.uid())::text
  and lower(storage.extension(name)) in ('jpg', 'jpeg', 'webp')
);

drop policy if exists "offer photos: delete from own folder" on storage.objects;
create policy "offer photos: delete from own folder"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'offer-photos'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);
