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

