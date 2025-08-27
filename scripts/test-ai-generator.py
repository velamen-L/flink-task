#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI编程脚手架测试脚本
验证生成器功能是否正常
"""

import json
import os
import sys
from pathlib import Path

def test_config_loading():
    """测试配置文件加载"""
    print("🔍 测试配置文件加载...")
    
    config_file = Path("scripts/examples/wrongbook-wide-table-config.json")
    if not config_file.exists():
        print("❌ 配置文件不存在")
        return False
    
    try:
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        required_fields = ["domain", "job_name", "event_types", "source_table", "result_table"]
        for field in required_fields:
            if field not in config:
                print(f"❌ 配置文件缺少必需字段: {field}")
                return False
        
        print("✅ 配置文件加载成功")
        print(f"   业务域: {config['domain']}")
        print(f"   作业名称: {config['job_name']}")
        print(f"   事件类型数量: {len(config['event_types'])}")
        return True
        
    except Exception as e:
        print(f"❌ 配置文件加载失败: {e}")
        return False

def test_template_loading():
    """测试模板文件加载"""
    print("\n🔍 测试模板文件加载...")
    
    template_file = Path("scripts/templates/dynamic-routing-template.json")
    if not template_file.exists():
        print("❌ 模板文件不存在")
        return False
    
    try:
        with open(template_file, 'r', encoding='utf-8') as f:
            template = json.load(f)
        
        if "routing_configs" not in template:
            print("❌ 模板文件缺少routing_configs字段")
            return False
        
        print("✅ 模板文件加载成功")
        print(f"   路由配置数量: {len(template['routing_configs'])}")
        return True
        
    except Exception as e:
        print(f"❌ 模板文件加载失败: {e}")
        return False

def test_ai_generator():
    """测试AI生成器"""
    print("\n🔍 测试AI生成器...")
    
    generator_file = Path("scripts/ai-job-generator.py")
    if not generator_file.exists():
        print("❌ AI生成器文件不存在")
        return False
    
    try:
        # 检查生成器是否可以导入
        import subprocess
        result = subprocess.run([
            sys.executable, str(generator_file), "--help"
        ], capture_output=True, text=True, timeout=10)
        
        if result.returncode == 0:
            print("✅ AI生成器运行正常")
            return True
        else:
            print(f"❌ AI生成器运行失败: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ AI生成器测试失败: {e}")
        return False

def test_file_structure():
    """测试文件结构"""
    print("\n🔍 测试文件结构...")
    
    required_files = [
        "src/main/java/com/flink/realtime/business/WrongbookWideTableApp.java",
        "sql/wrongbook_wide_table_catalog.sql",
        "scripts/ai-job-generator.py",
        "scripts/examples/wrongbook-wide-table-config.json",
        "scripts/templates/dynamic-routing-template.json",
        "docs/AI编程脚手架使用指南-简化版.md"
    ]
    
    missing_files = []
    for file_path in required_files:
        if not Path(file_path).exists():
            missing_files.append(file_path)
    
    if missing_files:
        print("❌ 缺少以下文件:")
        for file in missing_files:
            print(f"   - {file}")
        return False
    else:
        print("✅ 所有必需文件都存在")
        return True

def main():
    """主测试函数"""
    print("🚀 AI编程脚手架功能测试")
    print("=" * 50)
    
    tests = [
        ("文件结构检查", test_file_structure),
        ("配置文件加载", test_config_loading),
        ("模板文件加载", test_template_loading),
        ("AI生成器测试", test_ai_generator)
    ]
    
    passed = 0
    total = len(tests)
    
    for test_name, test_func in tests:
        print(f"\n📋 {test_name}")
        if test_func():
            passed += 1
        else:
            print(f"   ❌ {test_name} 失败")
    
    print("\n" + "=" * 50)
    print(f"📊 测试结果: {passed}/{total} 通过")
    
    if passed == total:
        print("🎉 所有测试通过！AI编程脚手架准备就绪")
        print("\n📚 下一步:")
        print("1. 运行: python scripts/ai-job-generator.py scripts/examples/wrongbook-wide-table-config.json")
        print("2. 查看生成的代码: ./generated/")
        print("3. 提交作业到Flink集群")
    else:
        print("⚠️  部分测试失败，请检查相关文件")
        return 1
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
