-- Invitees now accept or decline meetings. Meetings that already had an attendee were shown to them
-- without asking, so they count as accepted.

alter table meetings add column invite_status varchar(255);

alter table meetings add constraint ck_meetings_invite_status check (invite_status in ('PENDING', 'ACCEPTED'));

update meetings set invite_status = 'ACCEPTED' where attendee_id is not null;
