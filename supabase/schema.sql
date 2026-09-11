create extension if not exists "uuid-ossp";

create table if not exists public.debts (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid not null,
  name text not null,
  amount numeric(12,2) not null default 0,
  returned numeric(12,2) not null default 0,
  direction text not null check (direction in ('received', 'given')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.debts enable row level security;

grant usage, select, insert, update, delete on table public.debts to authenticated;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'debts_name_not_blank'
  ) then
    alter table public.debts
      add constraint debts_name_not_blank check (length(trim(name)) > 0);
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'debts_amount_non_negative'
  ) then
    alter table public.debts
      add constraint debts_amount_non_negative check (amount >= 0);
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'debts_returned_non_negative'
  ) then
    alter table public.debts
      add constraint debts_returned_non_negative check (returned >= 0);
  end if;

  if not exists (
    select 1
    from pg_constraint
    where conname = 'debts_returned_not_exceed_amount'
  ) then
    alter table public.debts
      add constraint debts_returned_not_exceed_amount check (returned <= amount);
  end if;
end $$;

drop policy if exists "Users can view own debts" on public.debts;
drop policy if exists "Users can insert own debts" on public.debts;
drop policy if exists "Users can update own debts" on public.debts;
drop policy if exists "Users can delete own debts" on public.debts;

create policy "Users can view own debts"
on public.debts for select
using (auth.uid() = user_id);

create policy "Users can insert own debts"
on public.debts for insert
with check (auth.uid() = user_id);

create policy "Users can update own debts"
on public.debts for update
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "Users can delete own debts"
on public.debts for delete
using (auth.uid() = user_id);

create or replace function public.handle_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger debts_updated_at
before update on public.debts
for each row
execute function public.handle_updated_at();

create or replace view public.debts_summary as
select
  user_id,
  direction,
  count(*) as count,
  sum(amount) as total_amount,
  sum(returned) as total_returned
from public.debts
group by user_id, direction;
