# FileWatcher - An Example of Spring Integration Application

**FileWatcher Application** is a Java-based program created to automatically monitor an "upload" folder for the arrival of large ZIP files. These files must have names that match specific templates. Once a file is fully copied into the folder, the application automatically extracts its contents and moves them to a designated destination based on the matching template.

This application is built using **Spring Boot** and **Spring Integration**.

---

## Table of Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [Setup](#setup)
4. [Configuration](#configuration)
5. [Usage](#usage)
6. [Testing](#testing)

---

## Features

- **File Monitoring:** Automatically detects ZIP files in the specified directory
- **Wait for Complete Arrival:** Integration Flow waits for large ZIP files to finish transferring by periodically checking their integrity, ensuring files are fully received before processing begins.
- **ZIP Integrity Check:** Validates the structural integrity of ZIP files using `java.util.zip.ZipFile`.
- **Stale File Detection:** Stale files are identified by comparing their last modification timestamp with the current time. These files will not be processed further to avoid errors.
- **File Processing:** Extracts valid ZIP files, processes their contents, and cleans up after processing.
- **Error Handling:** Moves invalid ZIP files to a designated "bad files" directory.
- **Spring Integration Flow:** Implements a seamless flow for file polling, processing, and handling using **Spring Integration**.

---

## Requirements

- **Java Version:** Java 17 or higher
- **Frameworks:**
    - **Spring Boot**
    - **Spring Integration**
    - **Lombok**
- **Build Tool:** Gradle

---

## Setup

1. Clone this repository:
   ```bash
   git clone git@github.com:consta/zipfilewatcher-example.git
   cd zipfilewatcher-example
   ```

2. Build the project:
   ```bash
   gradle build
   ```

2. Complete src/main/resources/application.yml and specify the properties for your environment see [Usage](#usage) section. You might want to refer to src/test/resources/application-test.yml

3. Build executable jar and run it or run the application with gradle in the project location
   ```bash
   gradle bootRun
   ```

---

## Configuration

The application behavior is managed through `FileIntegrationConfig` and externalized configurations. Below are some key details:

- **File Reading Source:** Watches a configured "input" folder for incoming ZIP files.
- **Bad Files Directory:** Customizable directory to relocate invalid ZIP files for further inspection.
- **Stale File Logic:** Compares the last modification timestamp of the files with the current time to identify files that are not actively being written into.
- **Spring Integration Flow:** Defines the end-to-end process workflow, including file polling, periodic validation of large files, parsing, and cleanup.
- **ZIP File Validation:** Ensures the structural integrity of ZIP files before extracting and processing them.

Custom behavioral parameters can be adjusted in the configuration (e.g., application yml file).

---

## Usage

1. **Define Input and Output Directories:**
   Ensure the directories for input files, processed files, and "bad files" are configured accordingly.

2. **Place Files:**
   Put ZIP files into the input directory for processing.

3. **Monitoring Progress:**
   The application automatically detects and processes the files, extracting valid ZIP contents, detecting stale ones, and relocating invalid files.

---

## Testing

The application includes various integrated tests to ensure reliability:

1. **Run Tests:** Start by running all tests with:
   ```bash
   ./gradle test
   ```

2. **Example Test:** `FileIntegrationConfigTest` checks the basic and alternative flows.

---

## Contributing

The author welcomes contributions to enhance the functionality! Fork this repository and submit a pull request or create issues for feature suggestions and bug fixes.

---
