-- The counter table is internal to the SECURITY DEFINER trigger only.
-- The trigger owner (postgres) retains access; app roles must not read or alter counters.
REVOKE ALL ON TABLE public.employe_compteurs FROM anon, authenticated;
