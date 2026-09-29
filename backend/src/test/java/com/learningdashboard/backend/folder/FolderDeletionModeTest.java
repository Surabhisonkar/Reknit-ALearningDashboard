package com.learningdashboard.backend.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FolderDeletionModeTest {

    @Test
    void parsesTheTwoAllowedValues() {
        assertThat(FolderDeletionMode.fromQueryValue("unfile")).isEqualTo(FolderDeletionMode.UNFILE);
        assertThat(FolderDeletionMode.fromQueryValue("delete")).isEqualTo(FolderDeletionMode.DELETE_CONCEPTS);
    }

    @Test
    void aMissingOrUnknownValueIsRejectedSoNothingIsDeletedByAccident() {
        assertThatThrownBy(() -> FolderDeletionMode.fromQueryValue(null)).isInstanceOf(InvalidFolderRequestException.class);
        assertThatThrownBy(() -> FolderDeletionMode.fromQueryValue("DELETE")).isInstanceOf(InvalidFolderRequestException.class);
        assertThatThrownBy(() -> FolderDeletionMode.fromQueryValue("")).isInstanceOf(InvalidFolderRequestException.class);
    }

    @Test
    void colourKeysRoundTripAndUnknownKeysAreRejected() {
        for (FolderColor color : FolderColor.values()) {
            assertThat(FolderColor.fromJson(color.key())).isEqualTo(color);
        }
        assertThatThrownBy(() -> FolderColor.fromJson("#ff0000")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void namesAreTrimmedAndCappedAt120Characters() {
        assertThat(Folder.normalizeName(null)).isEmpty();
        assertThat(Folder.normalizeName("  Biology ")).isEqualTo("Biology");
        assertThat(Folder.normalizeName("x".repeat(130))).hasSize(Folder.MAX_NAME_LENGTH);
    }
}
