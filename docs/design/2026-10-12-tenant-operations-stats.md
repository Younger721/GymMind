# 门店运营统计指标口径说明

## 目的

为健身房管理员提供与 AI「门店运营数据」工具一致的 REST 统计接口与页面，避免与旧版 `analytics/dashboard` 指标混用。

## 新接口（推荐）

| 接口 | 说明 |
|------|------|
| `GET /api/v1/analytics/tenant-operations/summary` | 周期汇总 |
| `GET /api/v1/analytics/tenant-operations/trend` | 按日/周/月趋势 |

权限：`analytics:read`，且需 **租户上下文**（店管理员 JWT 带 `tenantId`）。

### 签到人次（核心）

- **定义**：统计区间内 `gym_check_in` 表的 **记录条数**（`COUNT(*)`）。
- **含义**：同一会员多次到店会累计多次，适合衡量门店客流强度。
- **字段名**：`checkInPersonTimes`（API）/ 界面文案「签到人次」。

### 到店会员数（辅助）

- **定义**：区间内至少有一条签到记录的 **去重会员数**（`COUNT(DISTINCT member_id)`）。
- **字段名**：`uniqueVisitors`。

### 有效会员 / 会员总数

- **有效会员**：`member_profile.status = 'ACTIVE'` 的当前计数（与周期无关，快照）。
- **会员总数**：该租户下全部会员档案数。

### 较上期

- 默认对比 **与所选区间等长的上一段日历偏移**（完整自然月时对比上一自然月）。
- 提供 `checkInPersonTimesChangeRate`、`uniqueVisitorsChangeRate`（百分比，保留 1 位小数）。

## 旧接口（Legacy，仍保留）

`GET /api/v1/analytics/dashboard` 的 `checkInCount` / `checkInRate` 来自 **课程预约完成数**（`course_booking`），**不是**门店扫码/签到人次。

| 字段 | 数据源 | 说明 |
|------|--------|------|
| `checkInCount` | `course_booking` 已完成到店 | 课程维度 |
| `bookingRate` | 预约完成率 | 课程维度 |

新页面与首页「门店概览」**仅使用 tenant-operations 接口**。

## 与 AI 的一致性

`gym_operations_metrics` 插件与 `TenantOperationsQueryService` 共用同一套 SQL，因此助手回答中的「签到次数/人次」应与运营统计页数字一致（同一租户、同一日期区间）。

## 前端入口

- 首页 **门店概览**（店管理员 + `analytics:read`）
- 菜单 **数据分析** → **门店运营统计**（`/analytics`）
- **会员流失预警**（`/member-risks`）仍为独立名单页
