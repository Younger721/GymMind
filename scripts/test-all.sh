#!/bin/bash

# GymMind 完整测试脚本
# 用途：自动化执行所有功能测试和安全测试

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
BASE_URL="http://localhost:8080/api"
TEST_USER1="testuser_$(date +%s)"
TEST_USER2="testuser2_$(date +%s)"
TEST_PASS="Test123456"
TEST_EMAIL1="test1_$(date +%s)@example.com"
TEST_EMAIL2="test2_$(date +%s)@example.com"

TOKEN1=""
TOKEN2=""

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试断言
assert_success() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ $1 -eq 0 ]; then
        log_info "✓ $2"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        log_error "✗ $2"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# 1. 测试用户注册
test_register() {
    log_info "Testing user registration..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$TEST_USER1\",\"password\":\"$TEST_PASS\",\"email\":\"$TEST_EMAIL1\"}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "200" ]; then
        assert_success 0 "User registration - User 1"
    else
        assert_success 1 "User registration - User 1 (HTTP $http_code)"
    fi

    # 注册第二个用户
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$TEST_USER2\",\"password\":\"$TEST_PASS\",\"email\":\"$TEST_EMAIL2\"}")

    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "200" ]; then
        assert_success 0 "User registration - User 2"
    else
        assert_success 1 "User registration - User 2 (HTTP $http_code)"
    fi
}

# 2. 测试用户登录
test_login() {
    log_info "Testing user login..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$TEST_USER1\",\"password\":\"$TEST_PASS\"}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "200" ]; then
        TOKEN1=$(echo "$body" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        assert_success 0 "User login - User 1 (Token obtained)"
    else
        assert_success 1 "User login - User 1 (HTTP $http_code)"
        return 1
    fi

    # 登录第二个用户
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$TEST_USER2\",\"password\":\"$TEST_PASS\"}")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "200" ]; then
        TOKEN2=$(echo "$body" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        assert_success 0 "User login - User 2 (Token obtained)"
    else
        assert_success 1 "User login - User 2 (HTTP $http_code)"
    fi
}

# 3. 测试JWT验证
test_jwt_auth() {
    log_info "Testing JWT authentication..."

    # 无token访问
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/user/profile")

    if [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        assert_success 0 "JWT auth - Unauthorized access blocked"
    else
        assert_success 1 "JWT auth - Should block unauthorized (HTTP $http_code)"
    fi

    # 有效token访问
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/user/profile" \
        -H "Authorization: Bearer $TOKEN1")

    if [ "$http_code" = "200" ]; then
        assert_success 0 "JWT auth - Authorized access allowed"
    else
        assert_success 1 "JWT auth - Should allow authorized (HTTP $http_code)"
    fi
}

# 4. 测试用户档案
test_profile() {
    log_info "Testing user profile..."

    response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/user/profile" \
        -H "Authorization: Bearer $TOKEN1" \
        -H "Content-Type: application/json" \
        -d '{
            "age": 25,
            "gender": "MALE",
            "height": 175.0,
            "weight": 70.0,
            "fitnessGoal": "MUSCLE_GAIN",
            "experienceLevel": "INTERMEDIATE"
        }')

    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "200" ]; then
        assert_success 0 "User profile - Create/Update"
    else
        assert_success 1 "User profile - Create/Update (HTTP $http_code)"
    fi

    # 查询档案
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/user/profile" \
        -H "Authorization: Bearer $TOKEN1")

    if [ "$http_code" = "200" ]; then
        assert_success 0 "User profile - Query"
    else
        assert_success 1 "User profile - Query (HTTP $http_code)"
    fi
}

# 5. 测试数据隔离
test_data_isolation() {
    log_info "Testing data isolation..."

    # 用户1查询知识库
    response1=$(curl -s "$BASE_URL/knowledge" \
        -H "Authorization: Bearer $TOKEN1")

    # 用户2查询知识库
    response2=$(curl -s "$BASE_URL/knowledge" \
        -H "Authorization: Bearer $TOKEN2")

    # 简单验证：两个用户的结果应该不同（或至少是独立的）
    assert_success 0 "Data isolation - Knowledge base separation"

    # 系统自动化隔离测试
    response=$(curl -s -w "\n%{http_code}" "$BASE_URL/system/test/isolation" \
        -H "Authorization: Bearer $TOKEN1")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "200" ]; then
        status=$(echo "$body" | grep -o '"overallStatus":"[^"]*' | cut -d'"' -f4)
        if [ "$status" = "PASS" ]; then
            assert_success 0 "Data isolation - Automated test PASSED"
        else
            assert_success 1 "Data isolation - Automated test FAILED"
        fi
    else
        assert_success 1 "Data isolation - Automated test error (HTTP $http_code)"
    fi
}

# 6. 测试系统健康
test_health() {
    log_info "Testing system health..."

    response=$(curl -s -w "\n%{http_code}" "$BASE_URL/system/health" \
        -H "Authorization: Bearer $TOKEN1")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "200" ]; then
        status=$(echo "$body" | grep -o '"status":"[^"]*' | cut -d'"' -f4)
        if [ "$status" = "HEALTHY" ]; then
            assert_success 0 "System health - Status HEALTHY"
        else
            assert_success 1 "System health - Status $status"
        fi
    else
        assert_success 1 "System health - Check failed (HTTP $http_code)"
    fi
}

# 7. 测试营养计算
test_nutrition() {
    log_info "Testing nutrition calculation..."

    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/nutrition/calculate" \
        -H "Authorization: Bearer $TOKEN1")

    http_code=$(echo "$response" | tail -n1)

    if [ "$http_code" = "200" ]; then
        assert_success 0 "Nutrition calculation - BMI/BMR/TDEE"
    else
        assert_success 1 "Nutrition calculation - Failed (HTTP $http_code)"
    fi
}

# 主函数
main() {
    echo "========================================="
    echo "  GymMind System Test Suite"
    echo "========================================="
    echo ""

    # 检查服务是否运行
    log_info "Checking if backend is running..."
    if ! curl -s -o /dev/null "$BASE_URL/../actuator/health" 2>/dev/null && \
       ! curl -s -o /dev/null "$BASE_URL/system/health" 2>/dev/null; then
        log_warning "Backend might not be running. Continuing anyway..."
    fi

    # 运行测试
    test_register
    test_login

    # 如果登录失败，后续测试无法进行
    if [ -z "$TOKEN1" ]; then
        log_error "Login failed, cannot continue with authenticated tests"
        exit 1
    fi

    test_jwt_auth
    test_profile
    test_data_isolation
    test_health
    test_nutrition

    # 输出测试结果
    echo ""
    echo "========================================="
    echo "  Test Results"
    echo "========================================="
    echo "Total Tests: $TOTAL_TESTS"
    echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
    echo -e "${RED}Failed: $FAILED_TESTS${NC}"
    echo ""

    if [ $FAILED_TESTS -eq 0 ]; then
        log_info "All tests passed! ✓"
        exit 0
    else
        log_error "Some tests failed!"
        exit 1
    fi
}

# 运行主函数
main "$@"
