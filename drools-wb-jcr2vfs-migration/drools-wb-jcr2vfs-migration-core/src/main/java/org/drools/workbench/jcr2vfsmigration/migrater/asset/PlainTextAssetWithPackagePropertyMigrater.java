package org.drools.workbench.jcr2vfsmigration.migrater.asset;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.drools.guvnor.client.common.AssetFormats;
import org.drools.guvnor.client.rpc.Module;
import org.drools.guvnor.server.RepositoryAssetService;
import org.drools.repository.AssetItem;
import org.drools.workbench.jcr2vfsmigration.migrater.PackageImportHelper;
import org.drools.workbench.jcr2vfsmigration.migrater.util.DRLMigrationUtils;
import org.drools.workbench.jcr2vfsmigration.migrater.util.MigrationPathManager;
import org.drools.workbench.screens.drltext.service.DRLTextEditorService;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.base.options.CommentedOption;
import org.uberfire.java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;

@ApplicationScoped
public class PlainTextAssetWithPackagePropertyMigrater {

    protected static final Logger logger = LoggerFactory.getLogger( PlainTextAssetWithPackagePropertyMigrater.class );

    @Inject
    protected RepositoryAssetService jcrRepositoryAssetService;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    protected MigrationPathManager migrationPathManager;

    @Inject
    DRLTextEditorService drlTextEditorServiceImpl;

    @Inject
    PackageImportHelper packageImportHelper;

    public void migrate( Module jcrModule,
                         AssetItem jcrAssetItem ) {
        Path path = migrationPathManager.generatePathForAsset( jcrModule,
                                                               jcrAssetItem );
        final org.uberfire.java.nio.file.Path nioPath = Paths.convert( path );
        if ( !Files.exists( nioPath ) ) {
            ioService.createFile( nioPath );
        }

        StringBuilder sb = new StringBuilder();

        if ( AssetFormats.DRL.equals( jcrAssetItem.getFormat() ) ) {
            sb.append( "rule '" + jcrAssetItem.getName() + "'" );
            sb.append( "\n" );
            sb.append( "\n" );
        }
        sb.append( jcrAssetItem.getContent() );
        sb.append( "\n" );
        sb.append( "\n" );
        sb.append( "end" );

        String content = sb.toString();
       
        // Support for '#' has been removed from Drools Expert -> replace it with '//'
        if (AssetFormats.DSL.equals(jcrAssetItem.getFormat())
                || AssetFormats.DSL_TEMPLATE_RULE.equals(jcrAssetItem.getFormat())
                || AssetFormats.RULE_TEMPLATE.equals(jcrAssetItem.getFormat())
                || AssetFormats.DRL.equals(jcrAssetItem.getFormat())
                || AssetFormats.FUNCTION.equals(jcrAssetItem.getFormat())) {
            content = DRLMigrationUtils.migrateStartOfCommentChar(content);
        }
        
        String sourceWithImport = drlTextEditorServiceImpl.assertPackageName( content,
                                                                              path );
        sourceWithImport = packageImportHelper.assertPackageImportDRL( sourceWithImport,
                                                                       path );

        ioService.write( nioPath,
                         sourceWithImport,
                         new CommentedOption( jcrAssetItem.getLastContributor(),
                                              null,
                                              jcrAssetItem.getCheckinComment(),
                                              jcrAssetItem.getLastModified().getTime() ) );
    }

}
