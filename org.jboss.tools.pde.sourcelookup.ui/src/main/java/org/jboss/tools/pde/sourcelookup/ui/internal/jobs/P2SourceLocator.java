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

package org.jboss.tools.pde.sourcelookup.ui.internal.jobs;

import static org.jboss.tools.pde.sourcelookup.core.internal.utils.BundleUtil.getLocalSourcePath;
import static org.jboss.tools.pde.sourcelookup.core.internal.utils.BundleUtil.getLocalSourcePathIfExists;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.jboss.tools.pde.sourcelookup.core.internal.ISourceArtifactLocator;
import org.jboss.tools.pde.sourcelookup.core.internal.preferences.SourceLookupPreferences;
import org.jboss.tools.pde.sourcelookup.core.internal.utils.BundleUtil;

/**
 * Locates artifact sources from P2 repositories.
 *
 * @author Fred Bricon
 */
public class P2SourceLocator implements ISourceArtifactLocator {

  private List<String> blackList = Arrays.asList("org.eclipse.swt");

  @Override
  public IPath findSources(File jar, IProgressMonitor monitor) {
    if (!BundleUtil.isBundle(jar)) {
      return null;
    }
    IArtifactKey artifactKey = BundleUtil.getArtifactKey(jar);
    if (artifactKey == null || blackList.contains(artifactKey.getId())) {
      return null;
    }
    IArtifactKey sourceKey = BundleUtil.toSourceKey(artifactKey);

    ProvisioningUI provisioningUI = ProvisioningUI.getDefaultUI();

    List<URI> uris = Arrays
        .asList(provisioningUI.getRepositoryTracker().getKnownRepositories(provisioningUI.getSession()));
    Collections.sort(uris);// stupid trick to make eclipse.org repos being
    // searched almost first

    Path cacheFolder = SourceLookupPreferences.getInstance().getDownloadedSourcesDirectory();
    for (URI repo : uris) {
      if (monitor.isCanceled()) {
        return null;
      }
      IArtifactRepository artifactRepo = null;
      try {
        artifactRepo = provisioningUI.loadArtifactRepository(repo, false, monitor);
      } catch (ProvisionException ignored) {
        ignored.printStackTrace();
        // local urls seem to fail
      }
      if (artifactRepo == null || !artifactRepo.contains(sourceKey)) {
        continue;
      }

      IArtifactDescriptor[] results = artifactRepo.getArtifactDescriptors(sourceKey);
      if (results.length > 0) {
        try {
          return saveArtifact(artifactRepo, results[0], cacheFolder, monitor);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  private IPath saveArtifact(IArtifactRepository artifactRepo, IArtifactDescriptor artifactDescriptor, Path cacheFolder,
      IProgressMonitor monitor) throws IOException {
    Files.createDirectories(cacheFolder);
    IArtifactKey artifactKey = artifactDescriptor.getArtifactKey();
    Path sourcePath = getLocalSourcePath(cacheFolder, artifactKey);
    if (!Files.exists(sourcePath)) {
      File sourceFile = Files.createFile(sourcePath).toFile();
      try (FileOutputStream stream = new FileOutputStream(sourceFile)) {
        artifactRepo.getArtifact(artifactDescriptor, stream, monitor);
      }
    }
    return getLocalSourcePathIfExists(cacheFolder, artifactKey);
  }

}
