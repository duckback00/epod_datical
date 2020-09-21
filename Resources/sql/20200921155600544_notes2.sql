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

DECLARE
  v_column_exists number := 0;  
BEGIN
  Select count(*) into v_column_exists
    from user_tab_cols
    where upper(column_name) = 'STATE'
      and upper(table_name) = 'EMPLOYEES';
      --and owner = 'DELPHIXDB';  -- if using all_xxx or dba_xxx   
  if (v_column_exists = 0) then
      execute immediate 'alter table employees add (state varchar2(40))';
  end if;
end;
/
