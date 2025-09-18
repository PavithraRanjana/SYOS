#!/bin/bash

echo "Verifying SYOS Project Structure..."
echo "================================="

# Check main directories
directories=(
    "src/main/java/com/syos/domain/models"
    "src/main/java/com/syos/domain/valueobjects"
    "src/main/java/com/syos/domain/enums"
    "src/main/java/com/syos/repository/interfaces"
    "src/main/java/com/syos/repository/impl"
    "src/main/java/com/syos/service/interfaces"
    "src/main/java/com/syos/service/impl"
    "src/main/java/com/syos/controller"
    "src/main/java/com/syos/ui/interfaces"
    "src/main/java/com/syos/ui/impl"
    "src/main/java/com/syos/exceptions"
    "src/main/java/com/syos/utils"
    "src/test/java/com/syos"
    "sql"
)

all_good=true

for dir in "${directories[@]}"; do
    if [ -d "$dir" ]; then
        echo "✓ $dir"
    else
        echo "✗ $dir (MISSING)"
        all_good=false
    fi
done

# Check key files
files=(
    "pom.xml"
    "README.md"
    ".gitignore"
    "src/main/resources/application.properties"
    "src/main/java/com/syos/SyosApplication.java"
)

echo ""
echo "Key Files:"
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file (MISSING)"
        all_good=false
    fi
done

echo ""
if [ "$all_good" = true ]; then
    echo "🎉 Project structure is complete!"
    echo ""
    echo "Next steps:"
    echo "1. Copy the Java source files from the provided artifacts"
    echo "2. Copy the database schema to sql/schema.sql"
    echo "3. Update database connection settings in application.properties"
    echo "4. Run: ./build.sh"
    echo "5. Run: ./test.sh"
    echo "6. Run: ./run.sh"
else
    echo "❌ Project structure has missing components!"
fi
