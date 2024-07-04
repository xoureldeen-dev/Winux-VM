#!/bin/bash

# Directory where you want to extract the contents
output_dir="./extracted_contents"
bootstrap='bootstrap-2447-arm64-v8a.tar.gz'

# Ensure the output directory exists
mkdir -p "$output_dir"

# Get the absolute path of the output directory to avoid issues when changing directories
output_dir_abs=$(realpath "$output_dir")
if [ -f "./extracted_contents" ]; then
        echo "Extracted contents dir is exist, continue build.."
    else
        # Loop through all .deb files in the current directory
        for deb_file in *.deb; do
            echo "Processing $deb_file..."
        
            # Create a temporary directory for this .deb file
            temp_dir=$(mktemp -d)
        
            # Copy the .deb file into the temporary directory
            cp "$deb_file" "$temp_dir"
        
            # Change into the temporary directory
            cd "$temp_dir"
        
            # Extract the .deb file (an ar archive) in the current directory
            ar -xv "$(basename "$deb_file")"
        
            # Check if data.tar.xz exists, then extract it
            if [ -f "data.tar.xz" ]; then
                # Extract the contents of data.tar.xz into the output directory
                tar -xf "data.tar.xz" -C "$output_dir_abs"
            else
                echo "data.tar.xz not found in $deb_file"
            fi
        
            # Change back to the original directory
            cd - > /dev/null
        
            # Clean up the temporary directory
            rm -rf "$temp_dir"
            
            echo "Extraction complete."
            clear
        done
    fi
    
echo "Creating bootstraps."
cd ./extracted_contents/data/data/com.vectras.boxvidra/files
tar czvf ../../../../../$bootstrap *
cd
echo "Create bootstrap complete ($bootstrap)."