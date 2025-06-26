# GitHub Actions Setup Guide

This document explains the steps required to set up GitHub Actions CI/CD pipeline for the SMS Forward project.

## 🚀 Pipeline Features

### Main Workflow (build-and-release.yml)

- **Automatic testing**: Runs unit tests for every commit
- **Debug build**: Creates debug APK for pull requests and develop branch
- **Release build**: Creates release APK for main branch and tags  
- **GitHub Release**: Automatically creates releases when git tags are pushed
- **APK signing**: Produces signed APK with keystore

### CI Workflow (ci.yml)

- **Code quality**: Lint checks
- **Build verification**: Debug build validation
- **Security scan**: Security checks
- **Dependency check**: Dependency analysis

## 📋 Setup Steps

### 1. Configure Repository Secrets

Add the following secrets in your GitHub repository under `Settings > Secrets and Variables > Actions`:

```bash
# APK Signing (Optional)
KEYSTORE_BASE64       # Base64 encoded keystore file
KEYSTORE_PASSWORD     # Keystore password
KEY_ALIAS            # Key alias
KEY_PASSWORD         # Key password
```

### 2. Create Keystore (Optional)

Create a keystore to sign your APK:

```bash
# Create keystore
keytool -genkey -v -keystore sms-forward-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward-key

# Base64 encode (for GitHub secret)
# Windows
certutil -encode sms-forward-keystore.jks keystore-base64.txt

# Linux/Mac
base64 -i sms-forward-keystore.jks > keystore-base64.txt
```

### 3. Add Secrets

```bash
# Copy keystore-base64.txt content and add to KEYSTORE_BASE64 secret
# Add passwords used when creating the keystore to other secrets

KEYSTORE_BASE64: [keystore-base64.txt content]
KEYSTORE_PASSWORD: [keystore password]
KEY_ALIAS: sms-forward-key
KEY_PASSWORD: [key password]
```

## 🔄 Workflow Triggers

### Automatic Triggers

```bash
# Debug APK (Pull Request or develop branch)
git checkout develop
git add .
git commit -m "feat: new feature"
git push origin develop

# Release APK (main branch)
git checkout main
git merge develop
git push origin main

# GitHub Release (Git tag)
git tag v1.8.0
git push origin v1.8.0  # Creates automatic release
```

### Manual Trigger

You can manually run workflows from the `Actions` tab on GitHub.

## 📁 File Structure

```bash
.github/
├── workflows/
│   ├── build-and-release.yml    # Main CI/CD pipeline
│   └── ci.yml                   # Simple CI checks
└── SETUP.md                     # This file

app/
├── signing.gradle               # APK signing configuration
└── build.gradle                 # Main build file
```

## 🏗️ Build Configuration

### Adding to app/build.gradle

Add signing configuration to your main build file:

```gradle
// Add to the end of build.gradle (app level)
apply from: 'signing.gradle'
```

### Local Development

For local development, add signing settings in `gradle.properties`:

```properties
# gradle.properties (local - don't add to git!)
RELEASE_STORE_FILE=../path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

## 📦 APK Outputs

### Artifact Locations

```bash
# GitHub Actions Artifacts
debug-apk/           # Debug APK (PR and develop)
release-apk/         # Release APK (main and tag)
test-results/        # Test results
lint-results/        # Lint check results

# GitHub Releases (for tags)
SMS-Forward-v1.8.0.apk        # Signed release APK
SMS-Forward-v1.8.0-unsigned.apk # Unsigned APK (if no keystore)
SHA256SUMS                     # Hash verification file
```

### APK Naming

```bash
# Debug builds
SMS-Forward-v1.8.0-debug-20241226.apk

# Release builds  
SMS-Forward-v1.8.0-123-20241226.apk

# GitHub releases
SMS-Forward-v1.8.0.apk
```

## 🔒 Security

### Keystore Security

- ✅ Don't add keystore files to repository
- ✅ Store base64 encoded version in GitHub secrets
- ✅ Use environment variables for passwords
- ✅ Backup production keystore securely

### Secret Management

```bash
# Secure secrets
KEYSTORE_BASE64=xxxxx        # ✅ Base64 encoded keystore
KEYSTORE_PASSWORD=xxxxx      # ✅ Strong password
KEY_ALIAS=sms-forward-key    # ✅ Unique alias
KEY_PASSWORD=xxxxx           # ✅ Strong password

# INSECURE examples
KEYSTORE_PASSWORD=123456     # ❌ Weak password
KEY_ALIAS=key                # ❌ Generic alias
```

## 🧪 Testing

### Initial Test

```bash
# 1. Fork the repository
# 2. Set up secrets
# 3. Make a test commit

git checkout develop
echo "test" > test.txt
git add test.txt
git commit -m "test: GitHub Actions test"
git push origin develop

# 4. Check results in Actions tab
```

### Release Test

```bash
# For test release
git tag v1.8.0-test
git push origin v1.8.0-test

# Result: Test release should appear in Releases tab
```

## 🔧 Troubleshooting

### Common Errors

```bash
# Build errors
❌ SDK location not found
✅ Use actions/setup-java@v4

❌ Permission denied: gradlew
✅ chmod +x gradlew step is included

❌ Keystore not found
✅ Check KEYSTORE_BASE64 secret

❌ Version not found
✅ Check versionName in app/build.gradle
```

### Debug Tips

```bash
# Look for in workflow logs:
- "✅" marked successful steps
- "❌" marked failed steps  
- Environment variable values
- Build output details
```

## 📚 Advanced Features

### Parallel Builds

To build multiple variants simultaneously:

```yaml
strategy:
  matrix:
    variant: [debug, release]
```

### Conditional Deployment

To release only on specific branches:

```yaml
if: github.ref == 'refs/heads/main' && contains(github.event.head_commit.message, '[release]')
```

### Slack/Discord Notifications

Add webhook to send build results to Slack.

---

## 📞 Support

- **GitHub Issues**: For technical problems
- **Workflow Logs**: For error details in Actions tab
- **Documentation**: GitHub Actions official documentation
