import { OptionSection } from '@/pages/options/components/option-section';
import { PermissionOptionRow } from '@/pages/options/components/permission-option-row';

type PermissionsSectionProps = {
  readonly hasLoadedSnapshot: boolean;
  readonly isLoading: boolean;
  readonly logRedirect: boolean;
  readonly allowAllClasses: boolean;
  readonly logRedirectPending: boolean;
  readonly allowAllClassesPending: boolean;
  readonly setBooleanOption: (
    optionKey: 'logRedirect' | 'allowAllClasses',
    enabled: boolean
  ) => Promise<boolean>;
};

export function PermissionsSection({
  hasLoadedSnapshot,
  isLoading,
  logRedirect,
  allowAllClasses,
  logRedirectPending,
  allowAllClassesPending,
  setBooleanOption,
}: PermissionsSectionProps) {
  return (
    <OptionSection title="Permissions">
      {!hasLoadedSnapshot && isLoading ? (
        <p className="text-sm text-muted-foreground">Loading permissions...</p>
      ) : (
        <div className="flex flex-col gap-5">
          <PermissionOptionRow
            id="options-log-redirect"
            title="Log Redirect"
            description="Redirect MQS script logs into the integrated console output."
            checked={logRedirect}
            disabled={isLoading || logRedirectPending}
            onCheckedChange={(checked) => {
              void setBooleanOption('logRedirect', checked);
            }}
          />

          <PermissionOptionRow
            id="options-allow-all-classes"
            title="Allow All Classes"
            description="Allow scripts to access any Java class instead of only the MQS class whitelist."
            checked={allowAllClasses}
            disabled={isLoading || allowAllClassesPending}
            onCheckedChange={(checked) => {
              void setBooleanOption('allowAllClasses', checked);
            }}
          />
        </div>
      )}
    </OptionSection>
  );
}
