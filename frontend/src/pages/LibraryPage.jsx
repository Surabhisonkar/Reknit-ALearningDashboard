import { useState } from "react";

import {
  DeleteFolderDialog,
  FolderEditorModal,
  FolderStrip,
  LibraryConceptCard,
  LibrarySearchBox,
  MoveToFolderMenu,
  UNFILED,
  useLibrary,
} from "../features/library";
import { Button, OverflowMenu } from "../shared/ui";

/**
 * Library: folders as colour-coded "playlists" above a searchable grid.
 * Only one dialog is open at a time, tracked by `dialog`.
 */
function LibraryPage() {
  const library = useLibrary();
  const [dialog, setDialog] = useState(null); // { kind: "create" | "edit" | "delete" | "move", ... }
  const close = () => setDialog(null);

  if (library.status === "loading") {
    return (
      <main className="page-width library-page library-state">
        <p>Loading your library...</p>
      </main>
    );
  }

  if (library.status === "error") {
    return (
      <main className="page-width library-page library-state">
        <h1>Couldn't load your library</h1>
        <p>Please try refreshing the page.</p>
      </main>
    );
  }

  const { concepts, folders, selectedFolder, activeFolder, query, visibleConcepts } = library;

  return (
    <main className="page-width library-page">
      <header className="library-header">
        <h1>Library</h1>
        {concepts.length > 0 && <LibrarySearchBox value={query} onChange={library.setQuery} />}
      </header>

      {concepts.length === 0 && folders.length === 0 ? (
        <div className="library-empty">
          <p>You haven't saved any concepts yet.</p>
          <Button to="/create">Create your first concept</Button>
        </div>
      ) : (
        <>
          <FolderStrip
            folders={folders}
            totalCount={concepts.length}
            unfiledCount={library.unfiledCount}
            activeFolder={activeFolder}
            onSelect={library.setActiveFolder}
            onCreate={() => setDialog({ kind: "create" })}
          />

          {selectedFolder && (
            <div className="folder-bar">
              <h2>{selectedFolder.name}</h2>
              <OverflowMenu
                label={`Actions for folder ${selectedFolder.name}`}
                items={[
                  { label: "Rename or recolour", onSelect: () => setDialog({ kind: "edit", folder: selectedFolder }) },
                  { label: "Delete folder", onSelect: () => setDialog({ kind: "delete", folder: selectedFolder }), danger: true },
                ]}
              />
            </div>
          )}
          {activeFolder === UNFILED && (
            <div className="folder-bar">
              <h2>Unfiled</h2>
            </div>
          )}

          {visibleConcepts.length > 0 ? (
            <div className="library-grid">
              {visibleConcepts.map((concept) => (
                <LibraryConceptCard
                  key={concept.id}
                  concept={concept}
                  onMove={(c) => setDialog({ kind: "move", concept: c })}
                  onDelete={(c) => library.removeConcept(c.id)}
                />
              ))}
            </div>
          ) : (
            <div className="library-empty">
              {query ? (
                <>
                  <p>No concepts match “{query.trim()}”.</p>
                  <Button secondary small onClick={() => library.setQuery("")}>
                    Clear search
                  </Button>
                </>
              ) : (
                <p>
                  {selectedFolder
                    ? `Nothing in ${selectedFolder.name} yet. Use Move to folder on any card to add one.`
                    : "Nothing here yet."}
                </p>
              )}
            </div>
          )}
        </>
      )}

      {dialog?.kind === "create" && (
        <FolderEditorModal onClose={close} onSubmit={({ name, color }) => library.createFolder(name, color)} />
      )}
      {dialog?.kind === "edit" && (
        <FolderEditorModal
          folder={dialog.folder}
          onClose={close}
          onSubmit={({ name, color }) =>
            library.updateFolder(dialog.folder.id, {
              name: name !== dialog.folder.name ? name : undefined,
              color: color !== dialog.folder.color ? color : undefined,
            })
          }
        />
      )}
      {dialog?.kind === "delete" && (
        <DeleteFolderDialog
          folder={dialog.folder}
          onClose={close}
          onConfirm={({ deleteConcepts }) => library.deleteFolder(dialog.folder.id, { deleteConcepts })}
        />
      )}
      {dialog?.kind === "move" && (
        <MoveToFolderMenu
          concept={dialog.concept}
          folders={folders}
          onClose={close}
          onMove={async (folderId) => {
            await library.moveConcept(dialog.concept.id, folderId);
            close();
          }}
          onCreateAndMove={async (name, color) => {
            await library.createFolderAndMove(dialog.concept.id, name, color);
            close();
          }}
        />
      )}
    </main>
  );
}

export default LibraryPage;
