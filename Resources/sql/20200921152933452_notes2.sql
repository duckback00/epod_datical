BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE NOTES';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/

create table notes (id number(38), note varchar(200) );
insert into notes values (1,'Hello World');
insert into notes values (2,'Hey There!');
insert into notes values (3,'Hi Bye');
insert into notes values (4,'Copy-n-Paste');
insert into notes values (5,'More notes');
commit;

alter table employees add (state varchar2(30));
