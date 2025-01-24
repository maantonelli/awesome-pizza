--
-- Sample dataset containing a number of Hotels in various Cities across the world.
--

create sequence AWESOMESCHEMA.order_code start with 1 increment by 1;

-- =================================================================================================
-- CHEFS
insert into AWESOMESCHEMA.chef(first_name, last_name, created_date, last_modified_date)
values ('Gennaro', 'Esposito', CURRENT_DATE, CURRENT_DATE)
;

-- =================================================================================================
-- PIZZAS
insert into AWESOMESCHEMA.pizza(name, description, price, created_date, last_modified_date)
values ('Margherita', 'Pomodoro, mozzarella, basilico', 5, CURRENT_DATE, CURRENT_DATE),
       ('Paperino', 'Pomodoro, wustel, patatine', 6.5, CURRENT_DATE, CURRENT_DATE),
       ('Boscaiola', 'Mozzarella, panna, funghi, prosciutto cotto', 8, CURRENT_DATE, CURRENT_DATE),
       ('Patate e Salsiccia', 'Mozzarella, patate, salsiccia', 8, CURRENT_DATE, CURRENT_DATE)
;
