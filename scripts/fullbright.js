// @module(main=FullBrightHookModule, name=FullBright, version=1.0.3)

const Optional = importClass('java.util.Optional');
const Double = importClass('java.lang.Double');
const DoubleSliderCallbacks = importClass('net.minecraft.client.option.SimpleOption$DoubleSliderCallbacks');

const KEY_TOGGLE = MQS.keybinds.keys.G;
const FULLBRIGHT_VALUE = 10.0;
const KEYBIND_NAME = 'Toggle FullBright';

const CONFIG_KEY_ENABLED = 'isFullBrightActive';
const CONFIG_KEY_GAMMA = 'originalGamma';

const HOOK_METHOD_NAME = 'validate';

const MESSAGES = {
    ENABLED: "FullBright enabled. Press your bound key to toggle.",
    DISABLED: "FullBright disabled.",
    HOOK_INSTALLED: "Gamma validation hook installed.",
    HOOK_REMOVED: "Gamma validation hook removed.",
    BRIGHTNESS_ON: `FullBright ON (Value: ${FULLBRIGHT_VALUE})`,
    BRIGHTNESS_OFF: (gamma) => `FullBright OFF (Restored to ${gamma.toFixed(2)})`
};

class FullBrightHookModule {
    options = null;
    originalGamma = 0.5;
    isFullBrightActive = false;
    isHookActive = false;
    keybindDisposer = null;
    hookDisposer = null;


    onEnable() {
        this.options = this.getGameOptions();
        if (!this.options) {
            this.log("Could not enable: Game options not available.");
            return;
        }

        this.log(MESSAGES.ENABLED);

        this.originalGamma = MQS.config.getNumber(CONFIG_KEY_GAMMA, 0.5);
        this.isFullBrightActive = MQS.config.getBool(CONFIG_KEY_ENABLED, false);

        this.installHook();
        this.keybindDisposer = MQS.keybinds.bind(
            KEYBIND_NAME,
            KEY_TOGGLE,
            this.toggleFullBright.bind(this),
            MQS.keybinds.options()
                .repeatable(false)
                .build()
        );

        if (this.isFullBrightActive) {
            this.activateFullBright();
        }
    }

    onDisable() {
        if (this.isFullBrightActive) {
            this.deactivateFullBright();
        }
        this.removeHook();
        if (this.keybindDisposer) {
            this.keybindDisposer();
            this.keybindDisposer = null;
        }
        this.log(MESSAGES.DISABLED);
    }

    installHook() {
        if (this.isHookActive) return;

        this.hookDisposer = MQS.hooks.before(
            DoubleSliderCallbacks,
            HOOK_METHOD_NAME,
            this.onVerify.bind(this),
            { args: 1 }
        );

        this.isHookActive = true;
        this.log(MESSAGES.HOOK_INSTALLED);
    }


    onVerify(context, args, next) {
        const valueToValidate = args[0];
        return valueToValidate == null ? Optional.empty() : Optional.of(valueToValidate);
    }

    removeHook() {
        if (!this.isHookActive) return;

        if (this.hookDisposer) {
            this.hookDisposer();
            this.hookDisposer = null;
        }

        this.isHookActive = false;
        this.log(MESSAGES.HOOK_REMOVED);
    }

    toggleFullBright() {
        if (this.isFullBrightActive) {
            this.deactivateFullBright();
        } else {
            this.activateFullBright();
        }
        MQS.config.set(CONFIG_KEY_ENABLED, this.isFullBrightActive);
    }

    activateFullBright() {
        if (!this.options) return;

        this.originalGamma = this.options.getGamma().getValue();
        MQS.config.set(CONFIG_KEY_GAMMA, this.originalGamma);

        this.options.getGamma().setValue(Double.valueOf(FULLBRIGHT_VALUE));
        this.log(MESSAGES.BRIGHTNESS_ON);
        this.isFullBrightActive = true;
    }

    deactivateFullBright() {
        if (!this.options) return;

        if (this.originalGamma != null) {
            this.options.getGamma().setValue(Double.valueOf(this.originalGamma));
            this.log(MESSAGES.BRIGHTNESS_OFF(this.originalGamma));
        }
        this.isFullBrightActive = false;
    }

    getGameOptions() {
        const client = MQS.utils.mc.client();
        return client ? client.options : null;
    }

    log(message) {
        MQS.utils.chat.addInfoChatMessage(message, true);
    }
}

exportModule(FullBrightHookModule);
