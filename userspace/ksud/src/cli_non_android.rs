use anyhow::Result;
use clap::Parser;

use crate::{apk_sign, defs};

/// KernelSU cli for non-android
#[derive(Parser, Debug)]
#[command(author, version = defs::VERSION_NAME, about, long_about = None)]
struct Args {
    #[command(subcommand)]
    command: Commands,
}

#[derive(clap::Subcommand, Debug)]
enum Commands {
    /// Get apk size and hash
    GetSign {
        /// apk path
        apk: String,
    },
}

pub fn run() -> Result<()> {
    env_logger::init();

    let cli = Args::parse();

    log::info!("command: {:?}", cli.command);

    let result = match cli.command {
        Commands::GetSign { apk } => {
            let sign = apk_sign::get_apk_signature(&apk)?;
            println!("size: {:#x}, hash: {}", sign.0, sign.1);
            Ok(())
        }

    };

    if let Err(e) = &result {
        log::error!("Error: {e:?}");
    }
    result
}
