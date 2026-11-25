// downloads a test copy of vs code and runs suite.ts inside it with this extension loaded
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import { runTests } from "@vscode/test-electron";

async function main() {
  const extensionDevelopmentPath = path.resolve(__dirname, "..", "..");
  const extensionTestsPath = path.resolve(__dirname, "suite.js");
  // a short user data folder keeps vs code's socket path under the macos length limit
  const base = process.platform === "win32" ? os.tmpdir() : "/tmp";
  const userData = fs.mkdtempSync(path.join(base, "ckf-"));
  await runTests({
    extensionDevelopmentPath,
    extensionTestsPath,
    launchArgs: ["--disable-extensions", `--user-data-dir=${userData}`],
  });
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
