package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.PageResponse;
import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.dto.contact.ContactResponse;
import com.taja.crm.crm_backend.dto.customer.CustomerResponse;
import com.taja.crm.crm_backend.dto.metadata.FieldMetadata;
import com.taja.crm.crm_backend.dto.opportunity.OpportunityResponse;
import com.taja.crm.crm_backend.dto.search.EntitySearchRequest;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitySearchService {
    private final EntityManager em;
    private final EntityMetadataService metadata;
    private final UserRepository users;
    private static final Set<String> TEXT_OPS = Set.of("contains", "notContains", "equals", "notEquals", "startsWith", "empty", "notEmpty");
    private static final Set<String> EXACT_OPS = Set.of("equals", "notEquals", "empty", "notEmpty");
    private static final Set<String> ORDERED_OPS = Set.of("equals", "notEquals", "greaterThan", "greaterThanOrEqual", "lessThan", "lessThanOrEqual", "empty", "notEmpty");
    private static final Map<String, List<String>> KEYWORDS = Map.of(
            "leads", List.of("name", "company", "email", "phone", "owner", "source", "qualification.reason", "qualification.note"),
            "customers", List.of("name", "company", "email", "phone", "owner"),
            "contacts", List.of("name", "company", "email", "phone", "owner"),
            "opportunities", List.of("name", "owner"), "users", List.of("username", "email"), "roles", List.of("code", "name"));

    public PageResponse<?> search(String entity, String actorId, EntitySearchRequest request) {
        // 權限先於條件解析；不可被使用者的 OR 篩選繞過。
        if (actorId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if ("users".equals(entity)) {
            var actor = users.findById(actorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            if (actor.getRole() == null || !"ADMIN".equals(actor.getRole().getCode()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理員可以管理使用者");
        }
        if (!KEYWORDS.containsKey(entity)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 entity：" + entity);
        Plan plan = validate(entity, request);
        return switch (entity) {
            case "leads" -> query(Lead.class, plan, Function.identity());
            case "customers" -> query(Customer.class, plan, CustomerResponse::fromEntity);
            case "contacts" -> query(Contact.class, plan, ContactResponse::fromEntity);
            case "opportunities" -> query(Opportunity.class, plan, OpportunityResponse::fromEntity);
            case "users" -> query(User.class, plan, UserResponse::fromEntity);
            case "roles" -> query(Role.class, plan, RoleResponse::fromEntity);
            default -> throw new IllegalStateException();
        };
    }

    private record Field(String name, String path, String type, Map<String, String> options) {
        boolean text() { return Set.of("string", "text", "email", "phone").contains(type) && !"id".equals(name); }
        boolean number() { return "number".equals(type); }
        boolean date() { return "date".equals(type) || "dateTime".equals(type); }
    }
    private sealed interface Node permits Group, Rule {}
    private record Group(boolean all, List<Node> children) implements Node {}
    private record Rule(Field field, String operator, Object value) implements Node {}
    private record Sorting(Field field, boolean asc) {}
    private record Plan(int page, int size, String keyword, Node filter, List<Field> keywordFields, List<Sorting> sorts) {}

    private Plan validate(String entity, EntitySearchRequest request) {
        if (request.page() == null || request.page() < 0 || request.size() == null || request.size() < 1 || request.size() > 100)
            throw bad("page 必須大於等於 0，size 必須介於 1 到 100");
        if ((long) request.page() * request.size() > Integer.MAX_VALUE) throw bad("page 超出支援範圍");
        String keyword = request.keyword() == null ? "" : request.keyword().strip();
        if (keyword.length() > 200) throw bad("keyword 最多 200 字元");
        Map<String, Field> fields = new LinkedHashMap<>();
        for (FieldMetadata item : metadata.findFieldsByEntityName(entity)) {
            Map<String, String> options = new HashMap<>();
            if (item.options() != null) for (Option option : item.options())
                options.put(option.key(), "qualification.decision".equals(item.apiFieldName()) ? option.key() : option.value());
            String path = "users".equals(entity) && "roleId".equals(item.apiFieldName()) ? "role.id" : item.apiFieldName();
            fields.put(item.apiFieldName(), new Field(item.apiFieldName(), path, item.type(), options));
        }
        Node filter = request.filter() == null ? null : parse(request.filter(), fields, 0, new int[2]);
        List<Sorting> sorts = new ArrayList<>();
        Set<String> sorted = new HashSet<>();
        if (request.sort() != null) {
            if (request.sort().size() > 5) throw bad("最多 5 個排序欄位");
            for (var sort : request.sort()) {
                if (sort == null) throw bad("sort 不可包含 null");
                Field field = field(fields, sort.field());
                if (!"asc".equals(sort.direction()) && !"desc".equals(sort.direction())) throw bad("排序方向必須為 asc 或 desc");
                if (!sorted.add(field.name())) throw bad("排序欄位不可重複");
                sorts.add(new Sorting(field, "asc".equals(sort.direction())));
            }
        }
        if (!sorted.contains("id")) sorts.add(new Sorting(new Field("id", "id", "lookup", Map.of()), true));
        return new Plan(request.page(), request.size(), keyword, filter, KEYWORDS.get(entity).stream().map(fields::get).toList(), sorts);
    }

    private Node parse(Object raw, Map<String, Field> fields, int depth, int[] counts) {
        if (depth > 5) throw bad("filter 最多 5 層巢狀深度");
        if (++counts[0] > 200) throw bad("filter 最多 200 個節點");
        if (!(raw instanceof Map<?, ?> map)) throw bad("filter 節點必須是物件");
        if ("group".equals(map.get("kind"))) {
            if (!Set.of("kind", "match", "children").containsAll(map.keySet())) throw bad("group 包含不支援的欄位");
            if (!"all".equals(map.get("match")) && !"any".equals(map.get("match"))) throw bad("match 必須為 all 或 any");
            if (!(map.get("children") instanceof List<?> children)) throw bad("children 必須為陣列");
            if (children.isEmpty() && depth != 0) throw bad("不接受空的巢狀群組");
            List<Node> parsed = new ArrayList<>();
            for (Object child : children) parsed.add(parse(child, fields, depth + 1, counts));
            return new Group("all".equals(map.get("match")), parsed);
        }
        if (!"rule".equals(map.get("kind"))) throw bad("kind 必須為 group 或 rule");
        if (++counts[1] > 100) throw bad("filter 最多 100 條規則");
        if (!Set.of("kind", "field", "operator", "value").containsAll(map.keySet())) throw bad("rule 包含不支援的欄位");
        if (!(map.get("field") instanceof String name)) throw bad("field 必須為 apiFieldName");
        Field field = field(fields, name);
        if (!(map.get("operator") instanceof String operator)) throw bad(name + " 缺少 operator");
        Set<String> allowed = field.number() || field.date() ? ORDERED_OPS : field.text() ? TEXT_OPS : EXACT_OPS;
        if (!allowed.contains(operator)) throw bad(name + " 不支援 " + operator);
        if ("empty".equals(operator) || "notEmpty".equals(operator)) {
            if (map.containsKey("value")) throw bad(operator + " 不可帶 value");
            return new Rule(field, operator, null);
        }
        Object value = map.get("value");
        if (field.number()) {
            if (!(value instanceof Number)) throw bad(name + " 的 value 必須為 JSON number");
            try {
                BigDecimal number = new BigDecimal(value.toString());
                if (number.precision() > 100 || Math.abs((long) number.scale()) > 100)
                    throw bad(name + " 數值超出支援範圍");
                value = number;
            }
            catch (NumberFormatException e) { throw bad(name + " 的數值無效"); }
        } else {
            if (!(value instanceof String string)) throw bad(name + " 的 value 必須為字串");
            if (string.length() > 1000) throw bad("value 最多 1000 字元");
            if (field.date()) {
                if (!string.matches("\\d{4}-\\d{2}-\\d{2}")) throw bad(name + " 必須為 YYYY-MM-DD");
                try { value = LocalDate.parse(string); }
                catch (DateTimeParseException e) { throw bad(name + " 日期無效"); }
            } else if ("optionSet".equals(field.type())) {
                if (!field.options().containsKey(string)) throw bad(name + " 的選項 key 無效");
                value = field.options().get(string);
            } else value = field.text() ? string.strip().toLowerCase(Locale.ROOT) : string;
        }
        return new Rule(field, operator, value);
    }

    private Field field(Map<String, Field> fields, String name) {
        Field field = fields.get(name);
        if (field == null) throw bad("不支援的 field：" + name);
        return field;
    }
    private IllegalArgumentException bad(String message) { return new IllegalArgumentException(message); }

    private <T, R> PageResponse<R> query(Class<T> type, Plan plan, Function<T, R> mapping) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(type);
        Root<T> root = query.from(type);
        query.select(root).where(predicate(plan, root, cb));
        List<Order> orders = new ArrayList<>();
        for (Sorting sorting : plan.sorts()) {
            Expression<?> expression = expression(sorting.field(), root, cb);
            orders.add(cb.asc(cb.selectCase().when(cb.isNull(expression), 1).otherwise(0)));
            orders.add(sorting.asc() ? cb.asc(expression) : cb.desc(expression));
        }
        query.orderBy(orders);
        List<R> content = em.createQuery(query).setFirstResult(plan.page() * plan.size())
                .setMaxResults(plan.size()).getResultList().stream().map(mapping).toList();
        CriteriaQuery<Long> count = cb.createQuery(Long.class);
        Root<T> countRoot = count.from(type);
        count.select(cb.count(countRoot)).where(predicate(plan, countRoot, cb));
        long total = em.createQuery(count).getSingleResult();
        return PageResponse.fromPage(new PageImpl<>(content, PageRequest.of(plan.page(), plan.size()), total));
    }

    private Predicate predicate(Plan plan, Root<?> root, CriteriaBuilder cb) {
        Predicate filter = plan.filter() == null ? cb.conjunction() : predicate(plan.filter(), root, cb);
        if (plan.keyword().isEmpty()) return filter;
        String pattern = "%" + escape(plan.keyword().toLowerCase(Locale.ROOT)) + "%";
        Predicate[] keywords = plan.keywordFields().stream()
                .map(field -> cb.like(normalize(path(field, root), cb), pattern, '!')).toArray(Predicate[]::new);
        return cb.and(filter, cb.or(keywords));
    }
    private Path<?> path(Field field, Root<?> root) {
        if ("role.id".equals(field.path())) return root.join("role", JoinType.LEFT).get("id");
        Path<?> path = root;
        for (String part : field.path().split("\\.")) path = path.get(part);
        return path;
    }
    private Expression<String> normalize(Expression<?> path, CriteriaBuilder cb) {
        return cb.lower(cb.function("btrim", String.class, path, cb.literal(" \t\r\n\f")));
    }
    private Expression<?> expression(Field field, Root<?> root, CriteriaBuilder cb) {
        Path<?> path = path(field, root);
        if (field.text()) return normalize(path, cb);
        if ("dateTime".equals(field.type())) return cb.function("date", LocalDate.class, cb.nullif(normalize(path, cb), ""));
        return path;
    }
    private Predicate predicate(Node node, Root<?> root, CriteriaBuilder cb) {
        if (node instanceof Group group) {
            if (group.children().isEmpty()) return cb.conjunction();
            Predicate[] children = group.children().stream().map(child -> predicate(child, root, cb)).toArray(Predicate[]::new);
            return group.all() ? cb.and(children) : cb.or(children);
        }
        Rule rule = (Rule) node;
        Field field = rule.field();
        String op = rule.operator();
        Path<?> path = path(field, root);
        if ("empty".equals(op) || "notEmpty".equals(op)) {
            Predicate empty = field.number() || "date".equals(field.type()) ? cb.isNull(path)
                    : cb.or(cb.isNull(path), cb.equal(normalize(path, cb), ""));
            return "empty".equals(op) ? empty : cb.not(empty);
        }
        Expression<?> expression = expression(field, root, cb);
        return switch (op) {
            case "equals" -> cb.equal(expression, rule.value());
            case "notEquals" -> cb.notEqual(expression, rule.value());
            case "contains", "notContains", "startsWith" -> {
                String escaped = escape((String) rule.value());
                String pattern = "startsWith".equals(op) ? escaped + "%" : "%" + escaped + "%";
                Predicate like = cb.like(expression.as(String.class), pattern, '!');
                yield "notContains".equals(op) ? cb.not(like) : like;
            }
            default -> field.number()
                    ? compare(expression.as(BigDecimal.class), (BigDecimal) rule.value(), op, cb)
                    : compare(expression.as(LocalDate.class), (LocalDate) rule.value(), op, cb);
        };
    }
    private <T extends Comparable<? super T>> Predicate compare(Expression<T> expression, T value, String op, CriteriaBuilder cb) {
        return switch (op) {
            case "greaterThan" -> cb.greaterThan(expression, value);
            case "greaterThanOrEqual" -> cb.greaterThanOrEqualTo(expression, value);
            case "lessThan" -> cb.lessThan(expression, value);
            case "lessThanOrEqual" -> cb.lessThanOrEqualTo(expression, value);
            default -> throw bad("無效的比較運算子");
        };
    }
    private String escape(String value) { return value.replace("!", "!!").replace("%", "!%").replace("_", "!_"); }
}
