-- Initial entity schema. Existing installations must verify this schema before baselining at 1.

create table contacts (
    id varchar(255) not null,
    company varchar(255),
    customer_id varchar(255),
    email varchar(255),
    name varchar(255),
    owner varchar(255),
    phone varchar(255),
    primary key (id)
);

create table currencies (
    code varchar(3) not null,
    active boolean not null,
    fraction_digits integer not null,
    name varchar(255) not null,
    symbol varchar(255) not null,
    primary key (code)
);

create table customers (
    id varchar(255) not null,
    company varchar(255),
    email varchar(255),
    name varchar(255),
    owner varchar(255),
    phone varchar(255),
    primary key (id)
);

create table leads (
    id varchar(255) not null,
    company varchar(255),
    email varchar(255),
    name varchar(255),
    owner varchar(255),
    phone varchar(255),
    qualification_contact_id varchar(255),
    qualification_customer_id varchar(255),
    qualification_decision varchar(255),
    qualification_note text,
    qualification_opportunity_id varchar(255),
    qualification_reason text,
    qualification_reviewed_at varchar(255),
    source varchar(255),
    status varchar(255),
    primary key (id)
);

create table opportunities (
    id varchar(255) not null,
    amount numeric(19,2) not null,
    close_description varchar(2000),
    closed_at timestamp(6) with time zone,
    closed_by_id varchar(255),
    closed_by_name varchar(255),
    contact_id varchar(255),
    created_by_id varchar(255),
    customer_id varchar(255) not null,
    expected_close_date date,
    lead_id varchar(255),
    name varchar(255) not null,
    owner varchar(255),
    stage varchar(255) not null,
    primary key (id)
);

create table password_reset_tokens (
    id varchar(255) not null,
    expires_at timestamp(6) with time zone not null,
    token_hash varchar(64) not null unique,
    used boolean not null,
    user_id varchar(255) not null,
    primary key (id)
);

create table products (
    id varchar(255) not null,
    name varchar(255) not null,
    price numeric(19,2) not null,
    sku varchar(255) not null,
    status varchar(255) not null,
    primary key (id)
);

create table quote_audit (
    quote_id varchar(255) not null,
    action varchar(255) not null,
    actor_id varchar(255),
    actor_name varchar(255),
    occurred_at timestamp(6) with time zone not null,
    detail text,
    audit_id varchar(255) not null,
    version integer,
    audit_index integer not null check ((audit_index>=0)),
    primary key (quote_id, audit_index)
);

create table quote_lines (
    version_id varchar(255) not null,
    catalog_price numeric(19,2) not null,
    discount_cents bigint,
    discount_percent numeric(7,4) not null,
    line_id varchar(255) not null,
    product_id varchar(255) not null,
    product_name varchar(255) not null,
    quantity numeric(12,3) not null,
    sku varchar(255) not null,
    subtotal_cents bigint,
    tax_cents bigint,
    tax_percent numeric(7,4) not null,
    total_cents bigint,
    unit_price numeric(14,2) not null,
    line_index integer not null check ((line_index>=0)),
    primary key (version_id, line_index)
);

create table quote_versions (
    id varchar(255) not null,
    approval varchar(255) not null check ((approval in ('NotRequired','Required','Pending','Approved','Rejected'))),
    approval_at timestamp(6) with time zone,
    approval_by varchar(255),
    approval_reason text,
    approval_requested_at timestamp(6) with time zone,
    approval_requested_by varchar(255),
    created_at timestamp(6) with time zone not null,
    created_by varchar(255) not null,
    currency varchar(255) not null,
    decision_at timestamp(6) with time zone,
    decision_by varchar(255),
    decision_reason text,
    requires_reapproval boolean not null,
    reviewer_id varchar(255),
    reviewer_name varchar(255),
    revision bigint not null,
    sent_at timestamp(6) with time zone,
    status varchar(255) not null check ((status in ('Draft','Sent','Accepted','Rejected'))),
    delivery_terms text not null,
    name varchar(255) not null,
    notes text not null,
    payment_terms text not null,
    warranty text not null,
    discount_cents bigint,
    subtotal_cents bigint,
    tax_cents bigint,
    total_cents bigint,
    version integer not null,
    quote_id varchar(255) not null,
    customer_id varchar(255) not null,
    opportunity_id varchar(255),
    primary key (id),
    unique (quote_id, version)
);

create table quotes (
    id varchar(255) not null,
    created_by varchar(255) not null,
    number varchar(255) not null unique,
    order_id varchar(255),
    primary key (id)
);

create table refresh_tokens (
    id varchar(255) not null,
    expires_at timestamp(6) with time zone not null,
    family_id varchar(255) not null,
    revoked boolean not null,
    token_hash varchar(64) not null unique,
    token_version bigint not null,
    used_at timestamp(6) with time zone,
    user_id varchar(255) not null,
    primary key (id)
);

create table role_permissions (
    role_id varchar(255) not null,
    permission_code varchar(255) not null,
    unique (role_id, permission_code)
);

create table roles (
    id varchar(255) not null,
    code varchar(255) not null unique,
    description varchar(2000),
    name varchar(255) not null,
    quote_permission_version integer default 0 not null,
    revision bigint default 1 not null,
    primary key (id)
);

create table sales_order_audit (
    order_id varchar(255) not null,
    action varchar(255),
    actor_id varchar(255),
    actor_name varchar(255),
    at timestamp(6) with time zone,
    from_status varchar(255),
    id varchar(255),
    reason text,
    to_status varchar(255),
    audit_index integer not null check ((audit_index>=0)),
    primary key (order_id, audit_index)
);

create table sales_order_lines (
    order_id varchar(255) not null,
    catalog_price numeric(19,2) not null,
    discount_cents bigint,
    discount_percent numeric(7,4) not null,
    line_id varchar(255) not null,
    product_id varchar(255) not null,
    product_name varchar(255) not null,
    quantity numeric(12,3) not null,
    sku varchar(255) not null,
    subtotal_cents bigint,
    tax_cents bigint,
    tax_percent numeric(7,4) not null,
    total_cents bigint,
    unit_price numeric(14,2) not null,
    line_index integer not null check ((line_index>=0)),
    primary key (order_id, line_index)
);

create table sales_orders (
    id varchar(255) not null,
    cancellation_reason text,
    cancelled_at timestamp(6) with time zone,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone not null,
    created_by varchar(255) not null,
    currency varchar(255) not null,
    customer_name varchar(255),
    number varchar(255) unique,
    processing_at timestamp(6) with time zone,
    quote_number varchar(255),
    quote_version integer not null,
    quote_version_id varchar(255) not null,
    revision bigint not null,
    status varchar(255) not null,
    delivery_terms text not null,
    name varchar(255) not null,
    notes text not null,
    payment_terms text not null,
    warranty text not null,
    discount_cents bigint,
    subtotal_cents bigint,
    tax_cents bigint,
    total_cents bigint,
    updated_at timestamp(6) with time zone,
    quote_id varchar(255) not null unique,
    customer_id varchar(255) not null,
    opportunity_id varchar(255),
    primary key (id)
);

create table security_audit (
    id varchar(255) not null,
    action varchar(255),
    actor_id varchar(255),
    actor_name varchar(255),
    after_value text,
    at timestamp(6) with time zone,
    before_value text,
    target_id varchar(255),
    primary key (id)
);

create table user_password_history (
    user_id varchar(255) not null,
    password_hash varchar(255) not null,
    history_order integer not null check ((history_order>=0)),
    primary key (user_id, history_order)
);

create table users (
    id varchar(255) not null,
    email varchar(255) not null unique,
    enabled boolean default true not null,
    password_hash varchar(60) not null,
    token_version bigint default 0 not null,
    username varchar(255) not null unique,
    role_id varchar(255),
    primary key (id)
);

create index ix_refresh_family 
   on refresh_tokens (family_id);

alter table if exists password_reset_tokens 
   add constraint FKk3ndxg5xp6v7wd4gjyusp15gq 
   foreign key (user_id) 
   references users;

alter table if exists quote_audit 
   add constraint FKfl56sv0ycao9wt6c0ha484fop 
   foreign key (quote_id) 
   references quotes;

alter table if exists quote_lines 
   add constraint FKac3cbk329hr461inxbmbkutv2 
   foreign key (product_id) 
   references products;

alter table if exists quote_lines 
   add constraint FK4dyq4153fcfeu7t0sj4e92xid 
   foreign key (version_id) 
   references quote_versions;

alter table if exists quote_versions 
   add constraint FKhnp03x0qwwinnf7hph7je7o4j 
   foreign key (quote_id) 
   references quotes;

alter table if exists quote_versions 
   add constraint FKb0abwfb0wde3i7erlqk2hxgih 
   foreign key (customer_id) 
   references customers;

alter table if exists quote_versions 
   add constraint FKdpyj5uwsl4p19d6925o7cq9wr 
   foreign key (opportunity_id) 
   references opportunities;

alter table if exists role_permissions 
   add constraint FKn5fotdgk8d1xvo8nav9uv3muc 
   foreign key (role_id) 
   references roles;

alter table if exists sales_order_audit 
   add constraint FK3l4ugi62h3w0ibqd8p70ni6ey 
   foreign key (order_id) 
   references sales_orders;

alter table if exists sales_order_lines 
   add constraint FKimrho1hdy5hht88inb9a1m45o 
   foreign key (product_id) 
   references products;

alter table if exists sales_order_lines 
   add constraint FKn01j2wbyrxcpfo5ue94hx32om 
   foreign key (order_id) 
   references sales_orders;

alter table if exists sales_orders 
   add constraint FKk8in1aslfn4rjuwiewf85e3fl 
   foreign key (quote_id) 
   references quotes;

alter table if exists sales_orders 
   add constraint FKfs1owechmxg3lvej5vq1s8t8i 
   foreign key (customer_id) 
   references customers;

alter table if exists sales_orders 
   add constraint FK8diedoej5kbmbfw8qd5my8lle 
   foreign key (opportunity_id) 
   references opportunities;

alter table if exists user_password_history 
   add constraint FK5p57sn7crshs9l6jyl524ihg7 
   foreign key (user_id) 
   references users;

alter table if exists users 
   add constraint fk_users_role 
   foreign key (role_id) 
   references roles;
