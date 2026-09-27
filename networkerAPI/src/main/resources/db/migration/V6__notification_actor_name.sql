-- Notifications keep a copy of who sent them, so they still read right after that person deletes their account.
-- actor_id is cleared at that point; actor_name stays.

alter table notifications add column actor_name varchar(255);

update notifications n set actor_name = (select p.name from profiles p where p.id = n.actor_id);

alter table notifications alter column actor_id drop not null;
