# wrongbook Domain Request-based ER Diagram Report

**Generated**: Sat Sep 06 17:13:22 GMT+08:00 2025

## Statistics

- **Total Tables**: 5
- **Relations**: 0
- **Source Tables**: 1
- **Dimension Tables**: 4

## Source Tables

### wrongbook_fix

**Type**: SOURCE
**Connector**: kafka
**Comment**: Business event payload for wrongbook fix

| Field | Type | Key | Comment |
|-------|------|-----|----------|
| fix_id | VARCHAR(255) | PK | Primary key |
| wrong_id | VARCHAR(255) | FK | Foreign key |
| user_id | VARCHAR(255) | FK | Foreign key |
| subject | VARCHAR(255) | - |  |
| question_id | VARCHAR(255) | FK | Foreign key |
| pattern_id | VARCHAR(255) | FK | Foreign key |
| create_time | BIGINT | - |  |
| submit_time | BIGINT | - |  |
| fix_result | INT | - |  |

## Dimension Tables

### wrong_question_record

**Type**: DIMENSION
**Connector**: jdbc
**Comment**: 错题记录维表

| Field | Type | Key | Comment |
|-------|------|-----|----------|
| id | STRING | PK |  |
| user_id | STRING | - |  |
| question_id | STRING | - |  |
| pattern_id | STRING | - |  |
| subject | STRING | - |  |
| chapter_id | STRING | - |  |
| chapter_name | STRING | - |  |
| study_stage | STRING | - |  |
| course_type | STRING | - |  |
| answer_record_id | STRING | - |  |
| answer_image | STRING | - |  |
| result | TINYINT | - |  |
| correct_status | TINYINT | - |  |
| origin | STRING | - |  |
| tag_group | STRING | - |  |
| draft_image | STRING | - |  |
| q_type | INT | - |  |
| zpd_pattern_id | STRING | - |  |
| create_time | BIGINT | - |  |
| submit_time | BIGINT | - |  |
| is_delete | BOOLEAN | - |  |

### tower_pattern

**Type**: DIMENSION
**Connector**: jdbc
**Comment**: 知识点模式维�?

| Field | Type | Key | Comment |
|-------|------|-----|----------|
| id | STRING | PK |  |
| name | STRING | - |  |
| type | INT | - |  |
| subject | STRING | - |  |
| difficulty | DECIMAL | - |  |
| modify_time | BIGINT | - |  |

### tower_teaching_type_pt

**Type**: DIMENSION
**Connector**: jdbc
**Comment**: 教学类型-知识点映射维�?

| Field | Type | Key | Comment |
|-------|------|-----|----------|
| id | BIGINT | PK |  |
| teaching_type_id | BIGINT | - |  |
| pt_id | STRING | - |  |
| order_num | INT | - |  |
| is_delete | TINYINT | - |  |
| modify_time | TIMESTAMP(3) | - |  |

### tower_teaching_type

**Type**: DIMENSION
**Connector**: jdbc
**Comment**: 教学类型维表

| Field | Type | Key | Comment |
|-------|------|-----|----------|
| id | BIGINT | PK |  |
| chapter_id | STRING | - |  |
| teaching_type_name | STRING | - |  |
| is_delete | TINYINT | - |  |
| modify_time | TIMESTAMP(3) | - |  |

## Table Relations

| Source Table | Target Table | Source Field | Target Field | Relationship | Join Condition |
|--------------|--------------|--------------|--------------|--------------|----------------|

## Files Generated

- `wrongbook-request-based-er-diagram.mermaid` - Mermaid ER diagram (Request-based)
- `wrongbook-request-based-er-diagram.puml` - PlantUML ER diagram (Request-based)
- `wrongbook-request-based-er-report.md` - This comprehensive report
