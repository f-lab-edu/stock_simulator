
    create table accounts (
        is_active bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        money_value bigint,
        updated_at datetime(6),
        user_id bigint not null,
        account_number varchar(255) not null,
        bank_name varchar(255) not null,
        user_name varchar(255),
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table orders (
        deleted bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        total_price bigint,
        updated_at datetime(6) not null,
        user_id bigint not null,
        order_status enum ('CANCELED','COMPLETED','CREATED','PROCESSING') not null,
        order_type enum ('BUY','SELL') not null,
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table portfolio_stocks (
        id bigint not null auto_increment,
        last_updated_at datetime(6),
        money_value bigint,
        portfolio_id bigint not null,
        portfolio_quantity bigint not null,
        reserved_quantity bigint not null,
        stock_id bigint not null,
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table portfolios (
        available_cash bigint not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        portfolio_total_value bigint not null,
        reserved_cash bigint not null,
        updated_at datetime(6),
        user_id bigint not null,
        portfolio_type enum ('STOCK','TEST') not null,
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table stock_categories (
        id bigint not null auto_increment,
        category_name varchar(255) not null,
        description varchar(255),
        stock_category_type enum ('ETC','Energy','Finance','HealthCare','Technology','Utilities') not null,
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table stock_orders (
        average_executed_price bigint,
        created_at datetime(6) not null,
        executed_at datetime(6),
        executed_quantity bigint not null,
        id bigint not null auto_increment,
        order_id bigint not null,
        portfolio_id bigint not null,
        remained_quantity bigint not null,
        requested_price bigint not null,
        requested_quantity bigint not null,
        stock_id bigint not null,
        updated_at datetime(6) not null,
        stock_order_status enum ('CANCELLED','EXPIRED','FILLED','PARTIALLY_FILLED','PENDING') not null,
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table stocks (
        available_quantity bigint not null,
        id bigint not null auto_increment,
        stock_category_id bigint,
        stock_price bigint not null,
        stock_code varchar(255) not null,
        stock_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table trade (
        buy_order_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        last_updated_at datetime(6),
        sell_order_id bigint,
        stock_id bigint,
        trade_amount bigint not null,
        trade_price bigint not null,
        trade_quantity bigint not null,
        traded_at datetime(6),
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    create table users (
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        user_id varchar(30) not null,
        name varchar(50) not null,
        email varchar(255),
        location varchar(255),
        phone_number varchar(255),
        primary key (id)
    ) engine=InnoDB default charset=utf8mb4;

    alter table accounts
       add constraint UK6kplolsdtr3slnvx97xsy2kc8 unique (account_number);

    alter table portfolios
       add constraint UKpqkympi1xek834vclm10t7ypj unique (user_id, portfolio_type);

    alter table stock_categories
       add constraint UKgfr10566c8c8llugkic3qwvpi unique (category_name);

    alter table stocks
       add constraint UK86k9rdnbo6efhs39dotlr57rf unique (stock_code);

    alter table stocks
       add constraint UKpq0ii5jhrwhuxsqlio7dbc66s unique (stock_name);

    alter table trade
       add constraint uk_buy_sell_order unique (buy_order_id, sell_order_id);

    alter table accounts
       add constraint FKnjuop33mo69pd79ctplkck40n
       foreign key (user_id)
       references users (id);

    alter table orders
       add constraint FK32ql8ubntj5uh44ph9659tiih
       foreign key (user_id)
       references users (id);

    alter table portfolio_stocks
       add constraint FK9key7lkhq6rt42flierknviad
       foreign key (portfolio_id)
       references portfolios (id);

    alter table portfolio_stocks
       add constraint FKfj6ref2viw08erqb73vwybbod
       foreign key (stock_id)
       references stocks (id);

    alter table portfolios
       add constraint FK9xt36kgm9cxsf79r2me0d9f6u
       foreign key (user_id)
       references users (id);

    alter table stock_orders
       add constraint FK3c9i14djk3sl3olpmsa8v6sjt
       foreign key (order_id)
       references orders (id);

    alter table stock_orders
       add constraint FKclop1rumdm4yvnle0id9g34o0
       foreign key (portfolio_id)
       references portfolios (id);

    alter table stock_orders
       add constraint FKltns958hjsixp33ti3pvrg6ii
       foreign key (stock_id)
       references stocks (id);

    alter table stocks
       add constraint FK7sjbu50hbp6rld9ek79ctn9fa
       foreign key (stock_category_id)
       references stock_categories (id);

    alter table trade
       add constraint FKss253tph8vxgu5bjvqb0onxyb
       foreign key (buy_order_id)
       references stock_orders (id);

    alter table trade
       add constraint FKer8pswxnwh44rkalct3o3b75l
       foreign key (sell_order_id)
       references stock_orders (id);

    alter table trade
       add constraint FK852e1u5wkrpideinio6e1s9nl
       foreign key (stock_id)
       references stocks (id);
