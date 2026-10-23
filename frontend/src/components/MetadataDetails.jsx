/**
 * MetadataDetails: shows the EXIF metadata the backend extracted for ONE evidence record.
 * Presentational component (no state): it only displays the "item" prop it receives.
 */
export default function MetadataDetails({ item }) {
  // Old records (uploaded before Slice 3) have exifFound === null: we never checked them.
  // "===" compares value AND type, so null and false are treated differently.
  if (item.exifFound === null || item.exifFound === undefined) {
    return <p className="hint">Metadata was not extracted for this record (uploaded before Slice 3).</p>;
  }

  // filter(Boolean) drops null/empty parts; join(" ") glues the rest; "|| '-'" = fallback if nothing is left.
  // Template literals (backticks) build strings with ${...} placeholders.
  // Below, "?? '-'" means "use '-' only if the left side is null/undefined".
  const camera = [item.cameraMake, item.cameraModel].filter(Boolean).join(" ") || "-";
  const size = item.imageWidth && item.imageHeight ? `${item.imageWidth} x ${item.imageHeight} px` : "-";
  const hasGps = item.gpsLatitude != null && item.gpsLongitude != null;

  return (
    <div className="metadata section">
      <h3>Image metadata (EXIF)</h3>
      <dl>
        <dt>Dimensions</dt><dd>{size}</dd>
        <dt>Camera</dt><dd>{camera}</dd>
        <dt>Taken</dt><dd>{item.takenAt ?? "-"}</dd>
        <dt>Software</dt><dd>{item.software ?? "-"}</dd>
        <dt>GPS</dt>
        <dd>
          {hasGps
            ? `${item.gpsLatitude.toFixed(5)}, ${item.gpsLongitude.toFixed(5)}`
            : "-"}
        </dd>
      </dl>

      {/* Honest, neutral wording: absence of EXIF is a clue to note, never proof of fakery. */}
      {!item.exifFound && (
        <p className="hint">
          No EXIF data found. This is common for screenshots, WhatsApp/social-media images and
          AI-generated images, so it is <strong>not proof</strong> of manipulation. Note it for the human reviewer.
        </p>
      )}
      {item.software && (
        <p className="hint">
          A "Software" tag shows which program last wrote the file. Editing software is worth a closer look,
          but phones also write their own software name here.
        </p>
      )}
    </div>
  );
}
