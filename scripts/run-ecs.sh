#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

require_java21() {
  if ! command -v java >/dev/null 2>&1; then
    echo "未检测到 java，请先安装 JDK 21。"
    echo "Ubuntu/Debian 可尝试：apt install openjdk-21-jdk -y"
    exit 1
  fi

  if ! command -v javac >/dev/null 2>&1; then
    echo "未检测到 javac，请先安装 JDK 21，而不是只安装 JRE。"
    echo "Ubuntu/Debian 可尝试：apt install openjdk-21-jdk -y"
    exit 1
  fi

  local java_version
  java_version="$(java -version 2>&1 | head -n 1)"

  local javac_version
  javac_version="$(javac -version 2>&1)"

  if [[ "$javac_version" != *"21."* && "$javac_version" != "javac 21" ]]; then
    echo "当前 javac 版本不支持 Java 21：$javac_version"
    echo "请先安装并切换到 JDK 21，例如："
    echo "  apt install openjdk-21-jdk -y"
    echo "安装后再执行："
    echo "  java -version"
    echo "  javac -version"
    exit 1
  fi

  echo "当前 Java 版本：$java_version"
  echo "当前 Javac 版本：$javac_version"
}

if [[ ! -f ".env" ]]; then
  echo "未找到 .env，请先复制 .env.example 为 .env 并填写数据库连接。"
  exit 1
fi

if ! grep -Eq '^(DATABASE_DSN|PGDSN|MYSQL_DSN)=.+' .env; then
  echo ".env 中未找到可用的数据库连接，请先配置 DATABASE_DSN / PGDSN / MYSQL_DSN。"
  exit 1
fi

if [[ ! -x "./mvnw" ]]; then
  chmod +x ./mvnw
fi

require_java21

MAVEN_CMD="./mvnw"
if command -v mvn >/dev/null 2>&1; then
  MAVEN_CMD="mvn"
fi

echo "开始编译项目..."
"$MAVEN_CMD" clean compile

echo "启动网站..."
echo "默认访问地址: http://服务器公网IP:8080"
echo "如需修改端口，请编辑 .env 中的 APP_PORT"
exec "$MAVEN_CMD" exec:java
