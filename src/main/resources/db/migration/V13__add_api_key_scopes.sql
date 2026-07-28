-- Achado 4.1 (CRÍTICO): API key não tinha scopes granulares persistidos;
-- toda key recebia ROLE_API_CLIENT, que era aceito como bypass por @RequiresScope.
--
-- Chaves existentes ficam sem nenhum scope (string vazia) e passam a ser
-- negadas em qualquer endpoint anotado com @RequiresScope até que um
-- operador atribua scopes explicitamente. Isso é intencional: falhar fechado
-- é preferível a herdar acesso total implicitamente na migração.
ALTER TABLE api_keys
    ADD COLUMN scopes VARCHAR(500) NOT NULL DEFAULT '';

ALTER TABLE api_keys
    ALTER COLUMN scopes DROP DEFAULT;

COMMENT ON COLUMN api_keys.scopes IS
    'Lista de scopes concedidos à API key, separados por vírgula (ex.: "analysis:read,analysis:write"). Vazio = nenhum acesso a endpoints com @RequiresScope.';
