## 2.0.0
- Updated MaxMindDBReader to 2.0.0 to remove vulnerability due on jackson-databind
  - now required Java 8

## 1.0.2
- Update build to work with newer Logstash (tested with 7.17)
  - Updated for newer gradle
  - Modeled configuration from logstash-input-example_java_plugin
- No functional changes

## 1.0.1
- Disabled cache by default as it was shown not to result in any
  performance gain on a production dataset.

## 1.0.0
- Updated for GA release of native support for Java plugins. Includes:
  - Improved Gradle task wrappers
  - Removal of auto-generated Ruby source files 

## 0.2.0
- Updated for beta version of native support for Java plugins. Includes:
  - Gradle task wrappers
  - Updated plugin API
  - Full feature parity with Ruby plugins

## 0.0.1
- Initial version for experimental v0 of native support for Java plugins.
