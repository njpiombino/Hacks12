-- A PDF resume per profile. The file sits in its own table so profile queries don't load it.

alter table profiles add column resume_file_name varchar(255);
alter table profiles add column resume_size integer;
alter table profiles add column resume_uploaded_at timestamp(6) with time zone;

create table resumes (
    profile_id  uuid   not null,
    data        bytea  not null,
    constraint pk_resumes primary key (profile_id),
    constraint fk_resumes_profile foreign key (profile_id) references profiles (id) on delete cascade
);
