/* Copyright (c) 2012-2013, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.qtiworks.services;

import uk.ac.ed.ph.qtiworks.QtiWorksLogicException;
import uk.ac.ed.ph.qtiworks.QtiWorksRuntimeException;
import uk.ac.ed.ph.qtiworks.config.beans.QtiWorksDeploymentSettings;
import uk.ac.ed.ph.qtiworks.domain.entities.Assessment;
import uk.ac.ed.ph.qtiworks.domain.entities.AssessmentPackage;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateSession;
import uk.ac.ed.ph.qtiworks.domain.entities.Delivery;
import uk.ac.ed.ph.qtiworks.domain.entities.User;

import uk.ac.ed.ph.jqtiplus.internal.util.Assert;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to manage the creation and deletion of filespaces/sandboxes
 * for storing things like uploaded {@link AssessmentPackage}s and submitted files.
 * <p>
 * This is NO authorisation at this level.
 *
 * @author David McKain
 */
@Service
public final class FilespaceManager {

    private static final Logger logger = LoggerFactory.getLogger(FilespaceManager.class);

    @Resource
    private QtiWorksDeploymentSettings qtiWorksDeploymentSettings;

    @Resource
    private RequestTimestampContext requestTimestampContext;

    private String filesystemBaseUri;

    @PostConstruct
    public void init() {
        final String filesystemBaseString = qtiWorksDeploymentSettings.getFilesystemBase();
        final File filesystemBaseDirectory = new File(filesystemBaseString);
        if (!filesystemBaseDirectory.isDirectory()) {
            throw new QtiWorksRuntimeException("Filesystem base path " + filesystemBaseString + " is not a directory");
        }
        this.filesystemBaseUri = filesystemBaseDirectory.toURI().toString().replaceFirst("/$", "");
        logger.info("Filesystem base for client data is {}", filesystemBaseString);
    }

    public File createTempFile() {
        final String tmpFolderUri = filesystemBaseUri + "/tmp";
        final File tmpFolder = ensureCreateDirectory(tmpFolderUri);
        return new File(tmpFolder, createUniqueRequestComponent());
    }

    //-------------------------------------------------

    public File createAssessmentPackageSandbox(final User owner) {
        Assert.notNull(owner, "owner");
        final String filespaceUri = getAssessmentPackageSandboxBaseUri(owner)
                + "/" + createUniqueRequestComponent();
        return ensureCreateDirectory(filespaceUri);
    }

    public boolean deleteAssessmentPackageSandbox(final AssessmentPackage assessmentPackage) {
        Assert.notNull(assessmentPackage, "assessmentPackage");
        if (assessmentPackage.getSandboxPath()==null) {
            throw new IllegalStateException("Built-in AssessmentPackages may not be deleted");
        }
        return deleteAssessmentPackageSandbox(new File(assessmentPackage.getSandboxPath()));
    }

    public boolean deleteAssessmentPackageSandbox(final File sandboxDirectory) {
        Assert.notNull(sandboxDirectory, "sandboxDirectory");
        return recursivelyDeleteDirectory(sandboxDirectory);
    }

    public boolean deleteAssessmentPackageSandboxes(final User owner) {
        Assert.notNull(owner, "owner");
        return recursivelyDeleteDirectory(getAssessmentPackageSandboxBaseUri(owner));
    }

    public boolean deleteAllAssessmentPackages() {
        return recursivelyDeleteDirectory(getAssessmentPackageSandboxBaseUri());
    }

    private String getAssessmentPackageSandboxBaseUri(final User owner) {
        Assert.notNull(owner, "owner");
        return getAssessmentPackageSandboxBaseUri()
            + "/" + owner.getBusinessKey();
    }

    private String getAssessmentPackageSandboxBaseUri() {
        return filesystemBaseUri + "/assessments";
    }

    //-------------------------------------------------

    public File createCandidateUploadFile(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        final String uploadBaseUri = getCandidateSessionUploadBaseUri(candidateSession);
        final File candidateResponseFolder = ensureCreateDirectory(uploadBaseUri);
        return new File(candidateResponseFolder, createUniqueRequestComponent());
    }

    public boolean deleteCandidateUploads(final Delivery delivery) {
        Assert.notNull(delivery, "delivery");
        Assert.notNull(delivery.getAssessment(), "delivery.assessment");
        return recursivelyDeleteDirectory(getCandidateSessionUploadBaseUri(delivery));
    }

    public boolean deleteCandidateUploads(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        return recursivelyDeleteDirectory(getCandidateSessionUploadBaseUri(candidateSession));
    }

    public boolean deleteAllCandidateUploads() {
        return recursivelyDeleteDirectory(getCandidateUploadBaseUri());
    }

    private String getCandidateUploadBaseUri() {
        return filesystemBaseUri + "/responses";
    }

    private String getCandidateSessionUploadBaseUri(final Delivery delivery) {
        final Assessment assessment = delivery.getAssessment();

        return getCandidateUploadBaseUri()
                + "/assessment" + assessment.getId()
                + "/delivery" + delivery.getId();
    }

    private String getCandidateSessionUploadBaseUri(final CandidateSession candidateSession) {
        final User candidate = candidateSession.getCandidate();
        final Delivery delivery = candidateSession.getDelivery();

        return getCandidateSessionUploadBaseUri(delivery)
                + "/" + candidate.getBusinessKey()
                + "/session" + candidateSession.getId();
    }

    //-------------------------------------------------

    public File obtainCandidateSessionStateStore(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        return ensureCreateDirectory(getCandidateSessionStoreUri(candidateSession));
    }

    public boolean deleteCandidateSessionData(final Delivery delivery) {
        Assert.notNull(delivery, "delivery");
        return recursivelyDeleteDirectory(getCandidateSessionStoreBaseUri(delivery));
    }

    public boolean deleteCandidateSessionStore(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        return recursivelyDeleteDirectory(getCandidateSessionStoreUri(candidateSession));
    }

    public boolean deleteAllCandidateSessionData() {
        return recursivelyDeleteDirectory(getCandidateSessionStoreBaseUri());
    }

    private final String getCandidateSessionStoreBaseUri() {
        return filesystemBaseUri + "/sessions";
    }

    private final String getCandidateSessionStoreBaseUri(final Delivery delivery) {
        final Assessment assessment = delivery.getAssessment();
        final String folderBaseUri = getCandidateSessionStoreBaseUri()
                + "/assessment" + assessment.getId()
                + "/delivery" + delivery.getId();
        return folderBaseUri;
    }

    private final String getCandidateSessionStoreUri(final CandidateSession candidateSession) {
        final User candidate = candidateSession.getCandidate();
        final Delivery delivery = candidateSession.getDelivery();
        final String folderUri = getCandidateSessionStoreBaseUri(delivery)
                + "/" + candidate.getBusinessKey()
                + "/session" + candidateSession.getId();
        return folderUri;
    }

    //-------------------------------------------------

    /**
     * Deletes all assignment and candidate data from the system.
     * <p>
     * This corresponds to having the database schema wiped.
     * Use with caution!!!
     */
    public void deleteAllUserData() {
        deleteAllAssessmentPackages();
        deleteAllCandidateSessionData();
        deleteAllCandidateUploads();
    }

    //-------------------------------------------------

    /**
     * Prunes empty subdirectories within the QTIWorks file store.
     *
     * @return the total number of empty subdirectories deleted
     */
    public int purgeEmptyStoreDirectories() {
        int deletedCount;
        deletedCount = purgeStoreDirectoryIfEmpty(fileUriToFile(getCandidateSessionStoreBaseUri()));
        deletedCount += purgeStoreDirectoryIfEmpty(fileUriToFile(getCandidateUploadBaseUri()));
        deletedCount += purgeStoreDirectoryIfEmpty(fileUriToFile(getAssessmentPackageSandboxBaseUri()));
        return deletedCount;
    }

    private int purgeStoreDirectoryIfEmpty(final File directory) {
        if (!directory.exists()) {
            logger.warn("Store directory {} does not exist", directory);
            return 0;
        }
        /* Perform depth first search */
        int deletedCount = 0;
        final File[] childFiles = directory.listFiles();
        for (final File childFile : childFiles) {
            if (childFile.isDirectory()) {
                deletedCount += purgeStoreDirectoryIfEmpty(childFile);
            }
        }

        /* Then delete this directory if it is (now) empty */
        if (directory.listFiles().length == 0) {
            logger.debug("Deleting empty store directory {}", directory);
            if (directory.delete()) {
                ++deletedCount;
            }
            else {
                logger.warn("Unexpected failure to delete apparently empty store directory {}", directory);
            }
        }
        return deletedCount;
    }

    //-------------------------------------------------

    private final File ensureCreateDirectory(final String fileUri) {
        final File directory = fileUriToFile(fileUri);
        return ServiceUtilities.ensureDirectoryCreated(directory);
    }

    private final boolean recursivelyDeleteDirectory(final String fileUri) {
        return recursivelyDeleteDirectory(fileUriToFile(fileUri));
    }

    private final boolean recursivelyDeleteDirectory(final File directory) {
        if (directory.exists()) {
            /* Do sanity check */
            if (!directory.isDirectory()) {
                throw new QtiWorksLogicException("Expected " + directory.getAbsolutePath() + " to be a directory");
            }
            ServiceUtilities.recursivelyDelete(directory);
        }
        return true;
    }

    private File fileUriToFile(final String fileUri) {
        try {
            return new File(URI.create(fileUri));
        }
        catch (final RuntimeException e) {
            throw new QtiWorksLogicException("Unexpected failure parsing File URI " + fileUri);
        }
    }

    private String createUniqueRequestComponent() {
        final Date timestamp = requestTimestampContext.getCurrentRequestTimestamp();
        final long threadId = Thread.currentThread().getId();
        return new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(timestamp) + "-" + String.valueOf(threadId);
    }
}
