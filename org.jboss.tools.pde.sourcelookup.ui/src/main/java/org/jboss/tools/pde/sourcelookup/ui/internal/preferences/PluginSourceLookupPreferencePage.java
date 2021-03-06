/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.pde.sourcelookup.ui.internal.preferences;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.pde.sourcelookup.core.internal.preferences.SourceLookupPreferences;
import org.jboss.tools.pde.sourcelookup.ui.internal.UIActivator;

/**
 * @author Fred Bricon
 */
public class PluginSourceLookupPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public PluginSourceLookupPreferencePage() {
    super();
    setPreferenceStore(UIActivator.getInstance().getPreferenceStore());
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  protected void createFieldEditors() {
    BooleanFieldEditor reattachSourceOnStartup = new BooleanFieldEditor(
        SourceLookupPreferences.REATTACH_SOURCES_ON_STARTUP_KEY, "Re-Attach bundle sources on workbench startup",
        getFieldEditorParent());
    addField(reattachSourceOnStartup);

    AbsoluteDirectoryFieldEditor sourceDirectory = new AbsoluteDirectoryFieldEditor(
        SourceLookupPreferences.DEFAULT_SOURCES_DIRECTORY_KEY, "Sources cache directory", getFieldEditorParent());
    addField(sourceDirectory);
  }

  private static class AbsoluteDirectoryFieldEditor extends DirectoryFieldEditor {

    public AbsoluteDirectoryFieldEditor(String defaultSourcesDirectoryKey, String string, Composite fieldEditorParent) {
      super(defaultSourcesDirectoryKey, string, fieldEditorParent);
      setErrorMessage("The path to the cache folder must be absolute");
      setValidateStrategy(VALIDATE_ON_KEY_STROKE);
    }

    @Override
    protected boolean doCheckState() {
      Path p = Paths.get(getStringValue());
      if (!p.isAbsolute()) {
        return false;
      }
      if (Files.isRegularFile(p)) {
        setErrorMessage("The path must not point to an existing file");
        return false;
      }
      return true;
    }

  }

}
