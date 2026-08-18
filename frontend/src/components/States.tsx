type StateProps = {
  title?: string;
  message?: string;
};

export function LoadingState({
  title = "불러오는 중",
  message = "잠시만 기다려 주세요.",
}: StateProps) {
  return <StateCard tone="info" title={title} message={message} />;
}

export function EmptyState({
  title = "데이터 없음",
  message = "조회 조건에 맞는 결과가 없습니다.",
}: StateProps) {
  return <StateCard tone="muted" title={title} message={message} />;
}

export function ErrorState({
  title = "오류 발생",
  message = "요청을 처리하지 못했습니다.",
}: StateProps) {
  return <StateCard tone="error" title={title} message={message} />;
}

export function PermissionState({
  title = "권한 없음",
  message = "이 화면에 접근할 권한이 없습니다.",
}: StateProps) {
  return <StateCard tone="warning" title={title} message={message} />;
}

export function SuccessState({
  title = "처리 완료",
  message = "변경사항이 저장되었습니다.",
}: StateProps) {
  return <StateCard tone="success" title={title} message={message} />;
}

function StateCard({
  tone,
  title,
  message,
}: StateProps & { tone: "info" | "muted" | "error" | "warning" | "success" }) {
  const toneClasses = {
    info: "bg-lightinfo text-info",
    muted: "bg-lightgray text-muted",
    error: "bg-lighterror text-error",
    warning: "bg-lightwarning text-warning",
    success: "bg-lightsuccess text-success",
  }[tone];
  return (
    <section
      className={`rounded border border-ld p-6 shadow-sm ${toneClasses}`}
      role="status"
    >
      <h2 className="text-lg font-semibold text-dark">{title}</h2>
      <p className="mt-2 text-sm text-bodytext">{message}</p>
    </section>
  );
}
