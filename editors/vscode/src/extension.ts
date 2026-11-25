// fixes clojure documents with clj-kondo-fix. the document is piped through
// `clj-kondo-fix --stdin` from its own folder, so the project's .clj-kondo config applies.
// this is a command and an on-save hook rather than a formatter, so formatting stays with calva.
import * as vscode from "vscode";
import { spawn } from "child_process";
import * as fs from "fs";
import * as path from "path";

const TIMEOUT_MS = 30_000;
const CLOJURE_EXTENSIONS = new Set([".clj", ".cljs", ".cljc"]);

// apps started from the dock may not see the shell path, so check homebrew directly
const HOMEBREW_PATHS = ["/opt/homebrew/bin/clj-kondo-fix", "/usr/local/bin/clj-kondo-fix"];

export function activate(context: vscode.ExtensionContext) {
  const output = vscode.window.createOutputChannel("clj-kondo-fix");
  context.subscriptions.push(
    output,
    vscode.commands.registerCommand("clj-kondo-fix.fixFile", async () => {
      const document = vscode.window.activeTextEditor?.document;
      if (!document || !isClojure(document)) {
        return;
      }
      const edits = await fix(context, output, document);
      if (edits.length === 0) {
        vscode.window.setStatusBarMessage("clj-kondo-fix: nothing to fix", 3000);
        return;
      }
      const edit = new vscode.WorkspaceEdit();
      edit.set(document.uri, edits);
      await vscode.workspace.applyEdit(edit);
    }),
    vscode.workspace.onWillSaveTextDocument((event) => {
      const enabled = vscode.workspace.getConfiguration("clj-kondo-fix").get<boolean>("fixOnSave", false);
      if (enabled && isClojure(event.document)) {
        event.waitUntil(fix(context, output, event.document));
      }
    }),
  );
}

export function deactivate() {}

function isClojure(document: vscode.TextDocument): boolean {
  return document.languageId === "clojure" || CLOJURE_EXTENSIONS.has(path.extname(document.fileName).toLowerCase());
}

// the configured path, then the bundled binary, then clj-kondo-fix on the path or in homebrew
function findExecutable(context: vscode.ExtensionContext): string {
  const configured = vscode.workspace.getConfiguration("clj-kondo-fix").get<string>("path", "").trim();
  if (configured) {
    return configured;
  }
  const name = process.platform === "win32" ? "clj-kondo-fix.exe" : "clj-kondo-fix";
  const bundled = context.asAbsolutePath(path.join("bin", name));
  if (fs.existsSync(bundled)) {
    return bundled;
  }
  return HOMEBREW_PATHS.find((candidate) => fs.existsSync(candidate)) ?? "clj-kondo-fix";
}

function workingDirectory(document: vscode.TextDocument): string | undefined {
  if (document.uri.scheme === "file") {
    return path.dirname(document.uri.fsPath);
  }
  return vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
}

// the extension picks the language, so untitled documents are treated as .clj
function filename(document: vscode.TextDocument): string {
  const base = path.basename(document.fileName);
  return CLOJURE_EXTENSIONS.has(path.extname(base).toLowerCase()) ? base : "untitled.clj";
}

async function fix(
  context: vscode.ExtensionContext,
  output: vscode.OutputChannel,
  document: vscode.TextDocument,
): Promise<vscode.TextEdit[]> {
  const executable = findExecutable(context);
  const text = document.getText();
  try {
    const fixed = await run(executable, ["--stdin", "--filename", filename(document)], text, workingDirectory(document), output);
    if (fixed === text) {
      return [];
    }
    const whole = new vscode.Range(document.positionAt(0), document.positionAt(text.length));
    return [vscode.TextEdit.replace(whole, fixed)];
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    output.appendLine(message);
    vscode.window.showErrorMessage(`clj-kondo-fix: ${message}`);
    return [];
  }
}

// runs clj-kondo-fix with text on stdin and resolves with stdout; its log of
// applied fixes, printed on stderr, goes to the output channel
function run(
  executable: string,
  args: string[],
  text: string,
  cwd: string | undefined,
  output: vscode.OutputChannel,
): Promise<string> {
  return new Promise((resolve, reject) => {
    const child = spawn(executable, args, { cwd });
    let stdout = "";
    let stderr = "";
    const timer = setTimeout(() => {
      child.kill();
      reject(new Error(`did not finish within ${TIMEOUT_MS / 1000} seconds`));
    }, TIMEOUT_MS);
    child.stdout.on("data", (chunk) => (stdout += chunk));
    child.stderr.on("data", (chunk) => (stderr += chunk));
    child.on("error", (error) => {
      clearTimeout(timer);
      reject(new Error(`could not run ${executable}: ${error.message}`));
    });
    child.on("close", (code) => {
      clearTimeout(timer);
      if (stderr.trim()) {
        output.appendLine(stderr.trim());
      }
      if (code === 0) {
        resolve(stdout);
      } else {
        reject(new Error(stderr.trim() || `exited with status ${code}`));
      }
    });
    child.stdin.end(text);
  });
}
