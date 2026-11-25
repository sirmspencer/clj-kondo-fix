// runs inside vs code: fixes real documents through the extension
import * as assert from "assert";
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import * as vscode from "vscode";

const UNUSED_REQUIRE = "(ns example (:require [clojure.string :as s]))\n";

async function open(text: string): Promise<vscode.TextDocument> {
  const folder = fs.mkdtempSync(path.join(os.tmpdir(), "clj-kondo-fix-"));
  const file = path.join(folder, "example.clj");
  fs.writeFileSync(file, text);
  const document = await vscode.workspace.openTextDocument(file);
  await vscode.window.showTextDocument(document);
  return document;
}

export async function run(): Promise<void> {
  // CLJ_KONDO_FIX_TEST_PATH points at a development build; without it the bundled binary is used
  const executable = process.env.CLJ_KONDO_FIX_TEST_PATH ?? "";
  const config = vscode.workspace.getConfiguration("clj-kondo-fix");
  await config.update("path", executable, vscode.ConfigurationTarget.Global);
  console.log(`clj-kondo-fix under test: ${executable || "bundled"}`);
  const extension = vscode.extensions.getExtension("sirmspencer.clj-kondo-fix");
  assert.ok(extension, "extension is installed");
  await extension.activate();

  // the command fixes the document without saving it
  const commanded = await open(UNUSED_REQUIRE);
  await vscode.commands.executeCommand("clj-kondo-fix.fixFile");
  assert.strictEqual(commanded.getText(), "(ns example)\n");
  assert.ok(commanded.isDirty, "the fix is an unsaved edit");

  // a file with nothing to fix is left alone
  const clean = await open("(ns example)\n(defn f [] 1)\n");
  await vscode.commands.executeCommand("clj-kondo-fix.fixFile");
  assert.strictEqual(clean.getText(), "(ns example)\n(defn f [] 1)\n");

  // fix on save applies the fixes before the file is written
  await config.update("fixOnSave", true, vscode.ConfigurationTarget.Global);
  const saved = await open(UNUSED_REQUIRE);
  const editor = vscode.window.activeTextEditor!;
  await editor.edit((builder) => builder.insert(new vscode.Position(1, 0), "(defn f [] 1)\n"));
  await saved.save();
  assert.strictEqual(fs.readFileSync(saved.fileName, "utf8"), "(ns example)\n(defn f [] 1)\n");

  console.log("all clj-kondo-fix extension tests passed");
}
