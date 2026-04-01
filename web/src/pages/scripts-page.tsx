import { useMemo, useState } from 'react';
import { PowerOff, RefreshCw } from 'lucide-react';

import { MqsButton } from '@/components/shared/mqs-button';
import { MqsInput } from '@/components/shared/mqs-input';
import { MqsList } from '@/components/shared/mqs-list';
import { ScriptEntry } from '@/components/shared/script-entry';

type ScriptListItem = {
  id: string;
  name: string;
  version: string;
  path: string;
  enabled: boolean;
};

const MOCK_SCRIPTS: ReadonlyArray<ScriptListItem> = [
  {
    id: 'fullbright',
    name: 'FullBright',
    version: '1.0.3',
    path: 'C:/Users/titou/Documents/projets/minecraft_modding/mqs/MQS/scripts/fullbright.js',
    enabled: true,
  },
  {
    id: 'test-command-module',
    name: 'Test Command Module',
    version: '0.0.1',
    path: 'C:/Users/titou/Documents/projets/minecraft_modding/mqs/MQS/scripts/test-command.js',
    enabled: false,
  },
  {
    id: 'test-config-module',
    name: 'Test Config Module',
    version: '0.0.1',
    path: 'C:/Users/titou/Documents/projets/minecraft_modding/mqs/MQS/scripts/test-config.js',
    enabled: true,
  },
  {
    id: 'test-extends-module',
    name: 'Test Extends Module',
    version: '0.0.1',
    path: 'C:/Users/titou/Documents/projets/minecraft_modding/mqs/MQS/scripts/modules/test-extends.js',
    enabled: false,
  },
  {
    id: 'test-fabric-event',
    name: 'Test Fabric Event',
    version: '0.0.1',
    path: 'C:/Users/titou/Documents/projets/minecraft_modding/mqs/MQS/scripts/events/fabric/test-fabric-event.js',
    enabled: false,
  },
  {
    id: 'hud-helper',
    name: 'Hud Helper',
    version: '0.2.0',
    path: 'C:/Users/titou/Documents/projets/minecraft_modding/mqs/MQS/scripts/ui/hud/hud-helper.js',
    enabled: true,
  },
  {
    id: 'vein-miner',
    name: 'Vein Miner',
    version: '1.4.2',
    path: 'C:/Users/titou/Documents/projets/minecraft_modding/mqs/MQS/scripts/mining/tools/vein-miner.js',
    enabled: false,
  },
];

export function ScriptsPage() {
  const [searchValue, setSearchValue] = useState('');
  const [scripts, setScripts] = useState<Array<ScriptListItem>>([...MOCK_SCRIPTS]);

  const filteredScripts = useMemo(() => {
    const normalizedSearch = searchValue.trim().toLowerCase();

    if (!normalizedSearch) {
      return scripts;
    }

    return scripts.filter((script) => {
      return (
        script.name.toLowerCase().includes(normalizedSearch) ||
        script.version.toLowerCase().includes(normalizedSearch) ||
        script.path.toLowerCase().includes(normalizedSearch)
      );
    });
  }, [scripts, searchValue]);

  const handleScriptToggle = (scriptId: string, enabled: boolean) => {
    setScripts((currentScripts) => {
      return currentScripts.map((script) => {
        if (script.id !== scriptId) {
          return script;
        }

        return { ...script, enabled };
      });
    });
  };

  const handleRefresh = () => {
    setScripts([...MOCK_SCRIPTS]);
  };

  const handleDisableAll = () => {
    setScripts((currentScripts) => {
      return currentScripts.map((script) => ({ ...script, enabled: false }));
    });
  };

  return (
    <section className="flex h-full min-h-0 w-full flex-col items-start justify-start gap-4">
      <div className="w-full">
        <h2 className="text-left text-2xl font-semibold tracking-tight text-card-foreground">
          All your <span className="text-primary">QOL</span> Scripts!
        </h2>
      </div>

      <div className="flex w-full items-center gap-3">
        <MqsInput
          type="search"
          placeholder="Search a QOL script..."
          className="h-10 flex-1"
          value={searchValue}
          onChange={(event) => setSearchValue(event.target.value)}
        />

        <MqsButton
          type="button"
          variant="outline"
          size="icon"
          className="h-10 w-10 text-emerald-400 hover:text-emerald-300"
          aria-label="Refresh script list"
          onClick={handleRefresh}
        >
          <RefreshCw />
        </MqsButton>

        <MqsButton
          type="button"
          variant="outline"
          size="icon"
          className="h-10 w-10 text-rose-400 hover:text-rose-300"
          aria-label="Turn off all scripts"
          onClick={handleDisableAll}
        >
          <PowerOff />
        </MqsButton>
      </div>

      <MqsList className="w-full">
        {filteredScripts.length > 0 ? (
          filteredScripts.map((script) => {
            return (
              <ScriptEntry
                key={script.id}
                name={script.name}
                version={script.version}
                path={script.path}
                enabled={script.enabled}
                onEnabledChange={(enabled) => handleScriptToggle(script.id, enabled)}
              />
            );
          })
        ) : (
          <div className="rounded-xl border border-dashed border-border px-5 py-8 text-center text-sm text-muted-foreground dark:border-input">
            No scripts match your search.
          </div>
        )}
      </MqsList>
    </section>
  );
}
