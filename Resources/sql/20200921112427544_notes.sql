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
insert into notes values (3,'More notes');
commit;
