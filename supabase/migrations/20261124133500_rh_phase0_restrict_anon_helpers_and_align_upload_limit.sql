REVOKE EXECUTE ON FUNCTION public.mon_role() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.mon_role() TO authenticated;

REVOKE EXECUTE ON FUNCTION public.generer_matricule_employe() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.generer_matricule_employe() TO authenticated;

UPDATE storage.buckets
SET file_size_limit = 12582912
WHERE id = 'rh-prive'
  AND file_size_limit = 10485760;
